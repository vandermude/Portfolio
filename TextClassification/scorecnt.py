#!/usr/bin/env python

import sys
import math
import struct
import json
from collections import namedtuple
from readutils import read_model, read_data_labels
from classifier import classifier_tuples
from matthews import score_errors, score_positive, get_page_count, weighted_average

WeakLearner = namedtuple('WeakLearner', ['tuple', 'alpha', 'c1', 'c0'])

def main():
    """
    Command Line Inputs:
    Data file with lines: PAGE	URL	LABEL_1	LABEL_2	...	LABEL_n and associated text for page
    Input Model file with labels and array of weak learners
    Output the positive score TP / (TP + FP) for each label
    """
    model = read_model(sys.argv[2])
    # print 'model has {} labels and {} tuples'.format(len(model['labels']), len(model['learners']))
    page_tuples, page_labels, page_num_labels = read_data_labels(sys.argv[1], model)
    # print 'read {} pages'.format(len(page_labels))
    TP, TN, FP, FN = score_errors(model, page_tuples, page_labels, page_num_labels, False)
    score = score_positive(TP, TN, FP, FN)
    print 'score = {}'.format(score)
    page_total, page_count = get_page_count(page_labels)
    wgt_avg = weighted_average(page_total, page_count, score)
    print 'weighted average = {}'.format(wgt_avg)

if __name__ == '__main__':
    main()
