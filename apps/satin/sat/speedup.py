import fcntl
import FCNTL
import getopt
import os
import popen2
import select
import string
import sys
import threading
import time
import types

ibisdir = "~/ibis"

run_ibis = os.path.join( ibisdir, "bin", "run_ibis" )

logdir = "logs"

results = {}

#problem = "examples/qg/qg6-12.cnf.gz"
problem = "examples/qg/qg3-09.cnf.gz"
#problem = "examples/ais/ais10.cnf.gz"

#ProcNos = [ 1, 2, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40 ]
#ProcNos = [ 1, 2, 4, 8, 16, 32 ]
ProcNos = [ 2, 4, 8 ]
#ProcNos = [ 2 ]

nameserverport = 2001

# The timing result line starts with this string
timingTag = "ExecutionTime:"

class Thread( threading.Thread ):
    def  __init__( self, target, args=() ):
         if type( args ) <> types.TupleType:
            args = (args,)
         threading.Thread.__init__( self, target=target, args=args )

class LockedIterator:
    def __init__( self, iterator ):
        self._lock     = threading.Lock()
        self._iterator = iterator

    def __iter__( self ):
        return self

    def next( self ):
        try:
            self._lock.acquire()
            return self._iterator.next()
        finally:
            self._lock.release()

class MultiThread:
    def __init__( self, function, argsVector, maxThreads=5 ):
        self._function     = function
        self._argsIterator = LockedIterator( iter( argsVector ) )
        self._threadPool   = []
        for i in range( maxThreads ):
            self._threadPool.append( Thread( self._tailRecurse ) )

    def _tailRecurse( self ):
        for args in self._argsIterator:
            self._function( args ) 

    def start( self ):
        for thread in self._threadPool:
            time.sleep( 0 ) # necessary to give other threads a chance to run
            thread.start()

    def join( self, timeout=None ):
        for thread in self._threadPool:
            thread.join( timeout )

def get_time_stamp():
    return time.strftime( "%Y-%m-%d-%H:%M:%S", time.localtime())

def makeNonBlocking( fd ):
    fl = fcntl.fcntl(fd, FCNTL.F_GETFL)
    try:
	fcntl.fcntl(fd, FCNTL.F_SETFL, fl | FCNTL.O_NDELAY)
    except AttributeError:
	fcntl.fcntl(fd, FCNTL.F_SETFL, fl | FCNTL.FNDELAY)
    
# Run the given command. Return a tuple with the exit code, the stdout text,
# and the stderr text.
def getCommandOutput( command ):
    child = popen2.Popen3(command, 1) # capture stdout and stderr from command
    child.tochild.close()             # don't need to talk to child
    outfile = child.fromchild 
    outfd = outfile.fileno()
    errfile = child.childerr
    errfd = errfile.fileno()
    makeNonBlocking( outfd )            # don't deadlock!
    makeNonBlocking( errfd )
    outdata = errdata = ''
    outeof = erreof = 0
    while 1:
	ready = select.select([outfd,errfd],[],[]) # wait for input
	if outfd in ready[0]:
	    outchunk = outfile.read()
	    if outchunk == '': outeof = 1
	    outdata = outdata + outchunk
	if errfd in ready[0]:
	    errchunk = errfile.read()
	    if errchunk == '': erreof = 1
	    errdata = errdata + errchunk
	if outeof and erreof: break
	select.select([],[],[],.2) # give a little time for buffers to fill
    err = child.wait()
    return (err, outdata, errdata)

def build_run_command( pno, command, port ):
    return "prun -1 %s %d %d fs0.das2.cs.vu.nl %s -satin-closed" % (run_ibis, pno, port, command)

def runP( P, command ):
    cmd = build_run_command( P, command, nameserverport )
    print "Starting run for P=%d" % P
    data = getCommandOutput( cmd )
    results[P] = data
    print "Finished run for P=%d" % P

def report( msg, clients ):
    for f in clients:
        print >> f, msg

def reportTrace( txt, label, clients ):
    if txt == '':
        return
    report( "---- start of " + label + " ----", clients )
    l = txt.split( '\n' )
    for e in l:
        if e != '':
            report( e, clients )
    report( "---- end of " + label + " ----", clients )

def reportRun( label, data, clients ):
    (exitcode, out, err) = data
    if exitcode != 0:
        print "Command failed: exit code %d" % exitcode
    reportTrace( out, label + " output stream", clients )
    reportTrace( err, label + " error stream", clients )

# Given a line of the output, extract the execution time, or
# Null if it doesn't occur in this line.
def extractResultLine( l ):
    sz = len( timingTag )
    if l[0:sz] == timingTag:
       return l[sz:].strip()
    return None

# Given a results tuple, extract the execution time
def extractResult( data ):
    (exitcode, out, err) = data
    if exitcode != 0:
        # Run failed, no result.
        return None
    l = out.split( '\n' )
    for e in l:
       res = extractResultLine( e )
       if res != None:
           return res
    return None

def run( command, logfile, runParallel ):
    if logfile == None:
        logfile = os.path.join( logdir, "log-" + get_time_stamp() )
    lf = open( logfile, "w" )
    logstreams = [lf]
    allstreams = [lf,sys.stdout]
    report( "Command: " + command, allstreams )
    report( "Logfile: " + logfile, allstreams )
    report( "Tag: '" + timingTag + "'", allstreams )

    if runParallel != 0:
        mt = MultiThread( runP, ProcNos )
        mt.start()
        mt.join()
    else:
        for P in ProcNos:
            runP( P, command )
    report( " P time", allstreams )
    for P in ProcNos:
        res = extractResult( results[P] )
        report( "%2d %s" % (P, res), allstreams )
    for P in ProcNos:
        reportRun( "P=%d" % P, results[P], logstreams )
    lf.close()

def usage():
    print "Construct a table of execution times for different numbers of processors."
    print "Usage: python speedup.py [options] [program] [parameter...parameter]"
    print "The following options are supported:"
    print "--help\t\t\tShow this help text"
    print "-h\t\t\tShow this help text"
    print "-P\t\t\tExecute the runs in parallel"
    print "--logfile [name]\tUse the specified logfile"
    print "--logdir [name]\tUse the specified log directory"

def main():
    try:
        opts, args = getopt.getopt(sys.argv[1:], "hP", ["help", "logfile=", "logdir=", "tag="])
    except getopt.GetoptError:
        # print help information and exit:
        usage()
        sys.exit(2)
    logfile = None
    runParallel = 0
    for o, a in opts:
        #print "Option [%s][%s]" % (o, a)
        if o in ("-P", ):
            runParallel = 1
        if o in ("-h", "--help"):
            usage()
            sys.exit()
        if o in ("--tag",):
            timingTag = a
        if o in ("--logdir",):
            logdir = a
        if o in ("--logfile",):
            logfile = a
    run( string.join( args, ' ' ), logfile, runParallel )

if __name__ == "__main__":
    main()

