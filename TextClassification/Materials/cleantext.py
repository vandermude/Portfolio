#!/usr/bin/env python

import sys

def main():
    """
    Command Line Inputs:
    Data file with lines: PAGE	URL	LABEL_1	LABEL_2	...	LABEL_n and associated text for page
    Cleanedup data file
    """
    data_file = sys.argv[1]
    clean_file = sys.argv[2]
    with open(data_file, 'r') as input_file:
        with open(clean_file, 'w') as output_file:
            line_cnt = 0
            for line in input_file:
                line_cnt += 1
                new_line = ''.join('' if ord(char) >= 128 else char for char in line)
                # for i, char in enumerate(line):
                    # if ord(char) >= 128:
                        # print 'bad char {}={} line {} index {}'.format(char, ord(char), line_cnt, i)
                        # line.pop(i)
                output_file.write(new_line)

if __name__ == '__main__':
    main()
