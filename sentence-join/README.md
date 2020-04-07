# SENTENCE JOIN TOOL FOR PARACRAWL

written by Philipp Koehn, 2019

### Basic usage:
> ./sentence-join.py --train --model MY_MODEL --text MY_TRAINING_CORPUS

> ./sentence-join.py --tune  --model MY_MODEL --dev MY_DEV_CORPUS

> ./sentence-join.py --apply --model MY_MODEL --threshold MY_THRESHOLD < in > out

Input when applying is a pair of lines, separated by a tab character

Output is prediction if they should be joined


### Example: 
> This is a test <TAB> This is a test
> ===> True

> This is a test <TAB> and this is a test
> ===> False
