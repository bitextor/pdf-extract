#!/usr/bin/python3

import sys
import os
import subprocess
import argparse

kenlm_query = "/var/www/html/experiment/moses/bin/query"
kenlm_lmplz = "/var/www/html/experiment/moses/bin/lmplz"
kenlm_build_binary = "/var/www/html/experiment/moses/bin/build_binary"
break_token = "___BREAK___"

# SENTENCE JOIN TOOL FOR PARACRAWL
# written by Philipp Koehn, 2019
#
# Basic usage:
# ./sentence-join.py --train --model MY_MODEL --text MY_TRAINING_CORPUS
# ./sentence-join.py --tune  --model MY_MODEL --dev MY_DEV_CORPUS
# ./sentence-join.py --apply --model MY_MODEL --threshold MY_THRESHOLD < in > out
#
# Input when applying is a pair of lines, separated by a tab character
# Output is prediction if they should be joined
# 2020-06-01 Modified by Ramoelee : Add a new input parameter --kenlm_path (optional) for define the kenlm path.
# ./sentence-join.py --apply --model MY_MODEL --kenlm_path MY_KENLM_PATH

class KenLM(object):
  def __init__(self, cmd):
    self.cmd = cmd
    self.proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE)

  def process(self, line):
    input_string = u"%s\n" % line
    input_string = input_string.encode("utf-8")
    self.proc.stdin.write(input_string)
    self.proc.stdin.flush()
    result = self.proc.stdout.readline()
    #print(result.decode("utf-8").strip())
    return result.decode("utf-8").strip().split("\t")[4].split(" ")[2]

def train_models(text_file,model_stem):
  txt_fh = open(text_file,"r")
  forward_txt_fh  = open(model_stem + ".forward.tok",  "w")
  backward_txt_fh = open(model_stem + ".backward.tok", "w")
  line_buffer = []
  max_line_size = 100
  for line in txt_fh:
    if remove_punctuation:
      line_buffer.extend([word for word in line.split() if word != "." and word != "!" and word != "?"])
    else:
      line_buffer.extend(line.split())
    line_buffer.append(break_token)
    while len(line_buffer) > max_line_size:
      block = line_buffer[0:max_line_size]
      forward_txt_fh.write( " ".join(block) + "\n")
      block.reverse()
      backward_txt_fh.write(" ".join(block) + "\n")
      line_buffer = line_buffer[max_line_size:]
  forward_txt_fh.close()
  backward_txt_fh.close()
  
  for direction in ["forward","backward"]:
    os.system(kenlm_lmplz + " --order 5 --prune 0 0 1 -S 10G " + 
              "-T " + model_stem + ".tmp " +
              "--text " + model_stem + "." + direction + ".tok " + 
              "--arpa " + model_stem + "." + direction + ".lm")
    os.system(kenlm_build_binary + " -a 22 -q 8 -b 8 -S 10G " + 
              "-T " + model_stem + ".tmp " +
              "trie " +
              model_stem + "." + direction + ".lm " + 
              model_stem + "." + direction + ".binlm")

def score_context(left_context,left_no_break_token, right_context,right_no_break_token):
  left_context_no_break  = " ".join(left_context)  + " " + left_no_break_token
  right_context_no_break = " ".join(right_context) + " " + right_no_break_token
  left_context_break     = " ".join(left_context)  + " " + break_token
  right_context_break    = " ".join(right_context) + " " + break_token
  forward_predict_break     = kenlm_forward.process(left_context_break)
  forward_predict_no_break  = kenlm_forward.process(left_context_no_break)
  backward_predict_break    = kenlm_backward.process(right_context_break)
  backward_predict_no_break = kenlm_backward.process(right_context_no_break)
  #print(forward_predict_break,forward_predict_no_break,backward_predict_break,backward_predict_no_break)
  score =  float(forward_predict_break)  - float(forward_predict_no_break)
  score += float(backward_predict_break) - float(backward_predict_no_break)
  return score

def check_threshold(tune_set,threshold):
  correct_positive = 0
  correct_negative = 0
  false_positive = 0
  false_negative = 0
  for item in tune_set:
    prediction = item[0] > threshold
    if prediction and item[1]:
      correct_positive += 1
    elif prediction and not item[1]:
      false_positive += 1
    elif not prediction and not item[1]:
      correct_negative += 1
    else:
      false_negative += 1
    #print(item,correct_positive,correct_negative,false_positive,false_negative)
  precision = correct_positive / (correct_positive + false_positive + 0.01)
  recall = correct_positive / (correct_positive + false_negative + 0.01)
  f_measure = 2 * precision * recall / (precision + recall + 0.01)
  accuracy = (correct_positive + correct_negative) / len(tune_set)
  #print(threshold,accuracy,f_measure,precision,recall,correct_positive,correct_negative,false_positive,false_negative)
  return f_measure

def tune_threshold(tune_file):
  tune_fh = open(tune_file,"r")

  # load words from tune file
  words = []
  for line in tune_fh:
    if remove_punctuation:
      words.extend([word for word in line.split() if word != "." and word != "!" and word != "?"])
    else:
      words.extend(line.split())
    words.append(break_token)

  # create test instances
  tune_set = []
  for i in range(4,len(words)-4):
    left_context = []
    right_context = []
    for j in range(1,5):
      left_context.insert(0,words[i-j])
      right_context.insert(0,words[i+j])
    #print(left_context)
    #print(words[i])
    #print(right_context)

    if words[i] == break_token:
      left_no_break_token = words[i+1]
      right_no_break_token = words[i-1]
    else:
      left_no_break_token  = words[i]
      right_no_break_token = words[i]
    score = score_context(left_context,left_no_break_token, right_context,right_no_break_token)
    tune_set.append([score, words[i] == break_token])
  tune_fh.close()

  #print(len(tune_set))
  best_threshold = 0
  best_score = 0
  # grid search for best threshold
  for threshold in range(0,15):
    score = check_threshold(tune_set,threshold)
    if score > best_score:
      best_score = score
      best_threshold = threshold
  
  # binary search for better threshold
  for i in range(1,10):
    higher = best_threshold + 1/(2**i)
    lower  = best_threshold - 1/(2**i)
    for variant in [higher,lower]:
      score = check_threshold(tune_set,variant)
      if score > best_score:
        best_score = score
        best_threshold = variant

  print(best_score,best_threshold)
  return best_threshold

def runtime_scoring():
  for line in sys.stdin:
    #print( 0/0)
    #print(line.rstrip())
    try:
      (left_sentence,right_sentence) = line.rstrip().split("\t")
    except:
      print("Invalid input")
      sys.stdout.flush()
      continue
    left_words = left_sentence.split()
    right_words = right_sentence.split()

    left_context = []
    right_context = []
    for j in range(4):
      if j < len(left_words):
        left_context.insert(0,left_words[-j-1])
      else:
        left_context.insert(0,"___DUMMY___")
      if j < len(right_words):
        right_context.insert(0,right_words[j])
      else:
        right_context.insert(0,"___DUMMY___")
    #print(left_context, right_words[0], right_context, left_words[-1])
    score = score_context(left_context, right_words[0], right_context, left_words[-1])
    #print(score,threshold)
    #sys.stdout.flush()
    print(score < threshold)
    sys.stdout.flush()


parser = argparse.ArgumentParser(description='Command line tool for sentence joining decisions.')
parser.add_argument('--tune', help='tune the threshold on a sentence-split tune set', action="store_true")
parser.add_argument('--train', help='train the language models, provide sentence-split training set and prefix for models', action="store_true")
parser.add_argument('--apply', help='provide a pair of lines for splitting decisions on STDIN (tab-split, one pair per line)', action="store_true") 
parser.add_argument('--text', help='text file for training')
parser.add_argument('--dev', help='text file for tuning')
parser.add_argument('--model', help='prefix for models (expected extensions forward.binlm and backward.binlm)')
parser.add_argument('--kenlm_path', help='prefix for kenlm (expected extensions kenlm_query, kenlm_lmplz and kenlm_build_binary)')
parser.add_argument('--threshold', help='threshold for joining decisions', type=float)
parser.add_argument('--remove-punctuation', help='remove all punctuation from training and tuning text files', action="store_true")
args = parser.parse_args()

remove_punctuation = False
if args.remove_punctuation:
  remove_punctuation = args.remove_punctuation

# train language models
if args.train:
  if not args.text:
    print("ERROR: Specify a text file to train language models")
    parser.parse_args(["-h"])
  train_models(args.text,args.model)

# if tune or apply, then load model files
if args.tune or args.apply:
  if not args.model:
    print("ERROR: Specify a model prefix!")
    parser.parse_args(["-h"])
  if args.kenlm_path:
    kenlm_query = args.kenlm_path + "/query"
    kenlm_lmplz = args.kenlm_path + "/lmplz"
    kenlm_build_binary = args.kenlm_path + "/build_binary"
  #print(kenlm_query + "\n" + kenlm_lmplz + "\n" + kenlm_build_binary)
  kenlm_forward = KenLM([kenlm_query,"-b","-n",args.model + ".forward.binlm"])
  kenlm_backward = KenLM([kenlm_query,"-b","-n",args.model + ".backward.binlm"])

# optimize threshold
if args.tune:
  if not args.dev:
     print("ERROR: Specify a text file for tuning with --dev DEV!")
     parser.parse_args(["-h"])
  threshold = tune_threshold(args.dev)

# apply the model to stdin
if args.apply:
  threshold = 7.0
  if args.threshold:
    threshold = args.threshold
  elif os.path.exists(args.model + ".threshold"):
    fh = open(args.model + ".threshold", "r")
    threshold = float(fh.readlines()[0].rstrip().split(" ")[1])
  runtime_scoring()

# if no action is specified, complain
if not args.train and not args.tune and not args.apply:
  parser.parse_args(["-h"])

