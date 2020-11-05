#!/usr/bin/env python

import sys
import re
from matplotlib import pyplot as plt

def main():
    """
    Command Line Inputs:
    Input score[cnt, pos, NULL][test, train]all.dat
    Read all the data for the dataset [test2,3,4 train1,2,3,4]
    Create a plot score[cnt, pos, NULL][test, train]_[test2,3,4 train1,2,3,4].png
    For each model fraction [00 <-> 19]
    Plot colors:
    boost = red         [red dash 'r--']
    normalize = blue    [blue circle 'bo']
    naivebayes = green  [green triangle 'g^']
    cntnorm = black     [black | '|']
    or plot all five points 'o'
    plot five point average 'x-'
    """
    score_file = sys.argv[1]
    plot_file_prefix = score_file.replace('all.dat', '')
    if 'scorecnt' in score_file:
        models = ['boost', 'cntnorm', 'naivebayes']
    elif 'scorepos' in score_file:
        models = ['boost', 'cntnorm', 'naivebayes']
    else:
        models = ['boost', 'normalize']
    data_sets = ['test2', 'test3', 'test4', 'train1', 'train2', 'train3', 'train4']
    with open(score_file, 'r') as score:
        score_array = score.readlines()
    data_pt_x = {}
    data_pt_y = {}
    data_avg_x = {}
    data_avg_y = {}
    for data_set in data_sets:
        data_pt_x[data_set] = {}
        data_pt_y[data_set] = {}
        data_avg_x[data_set] = {}
        data_avg_y[data_set] = {}
        for model in models:
            data_pt_x[data_set][model] = []
            data_pt_y[data_set][model] = []
            # yes, this is inefficient - I know
            # a single pass would be harder to read
            for i in range(0, len(score_array), 3):
                line = score_array[i].strip()
                # add in 'boost' for model test
                if line.endswith('model'):
                    line = line + '.boost'
                if (model in line) and (data_set in line):
                    model_name = line.split(' ')[2]
                    fields = re.split(r'[_.]', model_name)
                    if 'model' in fields[2]:
                        continue
                    trunc = int(fields[2]) # model truncation [0 <-> 19]
                    # NOTE: score has trailing \n
                    score = float(score_array[i + 2].strip().replace('weighted average = ', ''))
                    data_pt_x[data_set][model].append(trunc)
                    data_pt_y[data_set][model].append(score)
            data_avg_x[data_set][model] = [0] * 20
            data_avg_y[data_set][model] = [0] * 20
            for x, y in zip(data_pt_x[data_set][model], data_pt_y[data_set][model]):
                data_avg_y[data_set][model][x] += y
            for x in range(20):
                data_avg_x[data_set][model][x] = x
                data_avg_y[data_set][model][x] /= 5.0
        plot_file = plot_file_prefix + '_' + data_set + '.png'
        fig, ax = plt.subplots()
        for model in models:
            if model == 'boost':
                label = 'boost'
                plotchar = 'o'
                plotcolor = 'red'
            elif model == 'cntnorm':
                label = 'normalize'
                plotchar = '*'
                plotcolor = 'blue'
            elif model == 'naivebayes':
                label = 'naivebayes'
                plotchar = '^'
                plotcolor = 'green'
            elif model == 'normalize':
                label = 'normalize'
                plotchar = 'x'
                plotcolor = 'black'
            ax.plot(data_pt_x[data_set][model], data_pt_y[data_set][model], plotchar, color=plotcolor, label=label)
            ax.plot(data_avg_x[data_set][model], data_avg_y[data_set][model], plotchar + '-', color=plotcolor)
            # add the legend with some customizations.
            legend = ax.legend(loc='upper center', shadow=True)
            # The frame is matplotlib.patches.Rectangle instance surrounding the legend.
            frame = legend.get_frame()
            frame.set_facecolor('0.90')
            # Set the fontsize
            for label in legend.get_texts():
                label.set_fontsize('large')
                for label in legend.get_lines():
                    label.set_linewidth(1.5)  # the legend line width
        plt.xlabel('tuples')
        plt.ylabel('score')
        plt.savefig(plot_file)
        plt.close()

if __name__ == '__main__':
    main()
