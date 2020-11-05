#!/usr/bin/env python

import sys
import re

def main():
    """
    Command Line Inputs:
    Input score[cnt, pos, NULL][test, train]all.dat
    Convert the score output to *.dat files usable by gnuplot
    This will be simple minded multipass scan of the input file
    ==========================================================
    scorecnt.py test2_0.test test2_0.00.model
    weighted average = 0.0531559420309
    BECOMES
    scorecnttest_boost_test2.dat: 0 00 0.0531559420309
    ====
    scorecnt.py test2_0.test test2_0.00.model.cntnorm
    weighted average = 0.0499223560412
    BECOMES
    scorecnttest_cntnorm_test2.dat: 0 00 0.0499223560412
    ==========================================================
    4 [boost, cntnorm, normalize, naivebayes] * 7 [test2,3,4 train1,2,3,4] *
        3 *[cnt, pos, NULL] * 2 [test, train]
    Total 56 *.dat files that become 14 graphs
    """
    read_file = sys.argv[1]
    output_file_prefix = read_file.replace('all.dat', '')
    if ('cnt' in read_file) or ('pos' in read_file):
        models = ['boost', 'normalize', 'cntnorm', 'naivebayes']
    else:
        models = ['boost', 'normalize']
    data_sets = ['test2', 'test3', 'test4', 'train1', 'train2', 'train3', 'train4']
    with open(read_file, 'r') as read:
        read_array = read.readlines()
    for model in models:
        for data_set in data_sets:
            output_file = output_file_prefix + '_' + model + '_' + data_set + '.dat'
            with open(output_file, 'w') as output:
                for i in range(0, len(read_array), 3):
                    line = read_array[i].strip()
                    # add in 'boost' for model test
                    if line.endswith('model'):
                        line = line + '.boost'
                    if (model in line) and (data_set in line):
                        model_name = line.split(' ')[2]
                        fields = re.split(r'[_.]', model_name)
                        split = fields[1]
                        trunc = fields[2]
                        # NOTE: score has trailing \n
                        score = read_array[i + 2].replace('weighted average = ', '')
                        if not trunc == 'model':
                            output.write('{}\t{}\t{}'.format(split, trunc, score))

if __name__ == '__main__':
    main()
