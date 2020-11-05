#!/usr/bin/env python

import sys
import json
import math
import operator
from collections import namedtuple
from readutils import read_model, read_data_text

WeakLearner = namedtuple('WeakLearner', ['tuple', 'alpha', 'c1', 'c0'])

def main():
    """
        Arguments
        argv[1] text to be classified
        argv[2] model
        argv[3] classification result
    """
    if len(sys.argv) != 4:
        print 'Usage: classifier.py data model output'
        sys.exit()
    pages = read_data_text(sys.argv[1])
    model = read_model(sys.argv[2])
    print 'model has {} labels and {} tuples'.format(len(model['labels']), len(model['learners']))
    label_names = [label.encode("utf8") for label in model['labels']]
    num_labels = len(model['labels'])
    print 'read {} pages'.format(len(pages))
    with open(sys.argv[3], 'w') as output_file:
        for page in pages:
            url, text = page.split('\t', 1)
            text = ' '.join(text.split())
            scores = classifier_text(model, text)
            label_scores = [[label_names[l], scores[l]] for l in range(num_labels)]
            # returns an array sorted by value
            sorted_label_scores = sorted(label_scores, key=lambda x: x[1], reverse=True)
            output_file.write('PAGE\t{}\t{}\n'.format(url, sorted_label_scores))

def classifier_text(model, text):
    if 'priors' in model:
        scores = bayesian_log_classifier_text(model, text)
        # scores = bayesian_classifier_text(model, text)
        return scores
    scores = None
    for learner in model['learners']:
        n_tuple = learner['tuple']
        if n_tuple in text:
            tuple_scores = [(learner['alpha'] * c) for c in learner['c1']]
        else:
            tuple_scores = [(learner['alpha'] * c) for c in learner['c0']]
        if scores is None:
            scores = tuple_scores
        else:
            scores = [(scores[i] + tuple_scores[i]) for i in range(len(scores))]
    return scores

# P(l|p) = P(l) * PRODUCT of (P(t|l) if t in p or (1 - P(t|l) if t not in p)
def bayesian_log_classifier_text(model, text):
    num_labels = len(model['labels'])
    scores = [math.log10(s) for s in model['priors']]
    for learner in model['learners']:
        n_tuple = learner['tuple']
        if n_tuple in text:
            tuple_score = [math.log10(c) for c in learner['c1']]
        else:
            tuple_score = [math.log10(1.0 - c) for c in learner['c1']]
        scores = [(scores[i] + tuple_score[i]) for i in range(num_labels)]
    return scores

# P(l|p) = P(l) * PRODUCT of (P(t|l) if t in p or (1 - P(t|l) if t not in p)
def bayesian_classifier_text(model, text):
    num_labels = len(model['labels'])
    scores = [s for s in model['priors']]
    for learner in model['learners']:
        n_tuple = learner['tuple']
        if n_tuple in text:
            tuple_score = [c for c in learner['c1']]
        else:
            tuple_score = [(1.0 - c) for c in learner['c1']]
        scores = [(scores[i] * tuple_score[i]) for i in range(num_labels)]
    return scores

def classifier_tuples(model, page_tuples, p):
    if 'priors' in model:
        scores = bayesian_classifier_tuples(model, page_tuples, p)
        return scores
    scores = None
    num_pages = len(page_tuples)
    num_tuples = len(model['learners'])
    for t in range(num_tuples):
        learner = model['learners'][t]
        if page_tuples[p][t] == 1:
            tuple_score = [(learner['alpha'] * c) for c in learner['c1']]
        else:
            tuple_score = [(learner['alpha'] * c) for c in learner['c0']]
        if scores is None:
            scores = tuple_score
        else:
            scores = [(scores[l] + tuple_score[l]) for l in range(len(scores))]
    return scores

# P(l|p) = P(l) * PRODUCT of (P(t|l) if t in p or (1 - P(t|l) if t not in p)
def bayesian_classifier_tuples(model, page_tuples, p):
    num_labels = len(model['labels'])
    num_tuples = len(model['learners'])
    scores = [s for s in model['priors']]
    for t in range(num_tuples):
        learner = model['learners'][t]
        if page_tuples[p][t] == 1:
            tuple_score = [c for c in learner['c1']]
        else:
            tuple_score = [(1.0 - c) for c in learner['c1']]
        scores = [(scores[i] * tuple_score[i]) for i in range(num_labels)]
    return scores

if __name__ == '__main__':
    main()
