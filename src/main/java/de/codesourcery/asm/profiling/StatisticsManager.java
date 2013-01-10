package de.codesourcery.asm.profiling;

import java.util.concurrent.ConcurrentHashMap;

import de.codesourcery.asm.rewrite.ProfilingRewriter;

/**
 * Gathers per-thread execution statistics (currently only the number of executed instructions).
 * 
 * @author tobias.gierke@code-sourcery.de
 * @see ProfilingRewriter
 * @see ExecutionStatistics 
 */
public class StatisticsManager
{
    /**
     * Defines how many instructions to execute before calling {@link #account()}.
     */
    public static final int GRANULARITY = 1; 
    
    private static final StatisticsManager INSTANCE = new StatisticsManager();
    
    private static final ConcurrentHashMap<Thread,Long> executionCounts = new ConcurrentHashMap<Thread,Long>();
    
    private static final ThreadLocal<ExecutionStatistics> statistics = new ThreadLocal<ExecutionStatistics>()  {
        
        protected ExecutionStatistics initialValue() {
            return new ExecutionStatistics() ;
        }
    };
    
    public StatisticsManager getInstance() {
        return INSTANCE;
    }
    
    public static long getExecutedInstructionsCount() {
        final Thread current = Thread.currentThread();
        Long existing = executionCounts.get( current );        
        return existing == null ? 0 : existing.longValue(); 
    }
    public static ExecutionStatistics getStatistics() {
        return statistics.get();
    }
    
    /**
     * Invoked periodically by generated byte-code whenever the {@link ExecutionStatistics#executedInstructionCount}
     * reaches zero or a positive value.
     * 
     */
    public static void account() 
    {
        final ExecutionStatistics stat = getStatistics();
        final Thread current = Thread.currentThread();
        Long existing = executionCounts.get( current );
        if ( existing == null ) {
            existing = Long.valueOf( stat.executedInstructionCount+GRANULARITY );
        } else {
            existing = Long.valueOf( existing.longValue() + stat.executedInstructionCount+GRANULARITY );
        }
        executionCounts.put( current , existing );
        
        // generated bytecode increments executedInstructionCount by the number of 
        // instructions in the current block and invokes account() whenever the
        // counter is >= 0
        stat.executedInstructionCount = -GRANULARITY;
    }
}