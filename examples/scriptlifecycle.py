# A script that demonstrates how the various parts of a script and
# their effects on worker threads. 

# The "top level" of the script is called once for each worker
# process. Perform any one-off initialisation here. For example,
# import all the modules, set up shared data structures, and declare
# all the Test objects you will use.

from java.lang import System

# The totalNumberOfRuns variable is shared by all worker threads.
totalNumberOfRuns = 0

# An instance of the TestRunner class is created for each worker thread.
class TestRunner:

    # There's an runsForThread variable for each worker thread.
    runsForThread = 0
    
    # The __init__ method is called once for each thread.
    def __init__(self):
        # There's an initialisationTime variable for each worker thread.
        self.initialisationTime = System.currentTimeMillis()

        grinder.logger.output("New thread started at time %s" %
                              self.initialisationTime)

    # The __call__ method is called once for each test run performed by
    # a worker thread.
    def __call__(self):

        # We really should synchronise this access to the shared
        # totalNumberOfRuns variable. See JMS receiver example for how
        # to use the Python Condition class.
        global totalNumberOfRuns
        totalNumberOfRuns += 1

        self.runsForThread += 1

        grinder.logger.output(
            "runsForThread=%d, totalNumberOfRuns=%d, initialisationTime=%d" %
            (self.runsForThread, totalNumberOfRuns, self.initialisationTime))
        
        # You can also vary behaviour based on thread ID.
        if grinder.threadID % 2 == 0:
            grinder.logger.output("I have an even thread ID.")
