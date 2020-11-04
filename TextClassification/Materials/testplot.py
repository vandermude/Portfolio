#!/usr/bin/env python

from matplotlib import pyplot

def main():
    f = pyplot.plot([1,2,3,4,5,6,5,10], [11,12,31,44,0,-6,5,110], 'o-', color='red')
    pyplot.show()
    pyplot.savefig('testplot.png')

if __name__ == '__main__':
    main()
