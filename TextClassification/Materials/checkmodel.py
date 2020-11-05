#!/usr/bin/env python

import sys
import math
import struct
import json
from collections import namedtuple
from copy import deepcopy
from readutils import read_model, write_model, read_data_labels
from classifier import classifier_tuples
from matthews import score_errors, score_matthews, get_page_count, weighted_average

WeakLearner = namedtuple('WeakLearner', ['tuple', 'alpha', 'c1', 'c0'])

def main():
    """
    Command Line Inputs:
    Input Model file with labels and array of weak learners
    """
    filename = sys.argv[1]
    print 'open {}'.format(filename)
    model = read_model(filename)
    print 'model has {} labels and {} tuples'.format(len(model['labels']), len(model['learners']))

if __name__ == '__main__':
    main()
