package org.eclipse.jifa.worker.vo.heapdump.thread;

import java.util.List;

import lombok.Data;

@Data
public class StackTrace {
    List<StackFrame> trace;
    
    public StackTrace(List<StackFrame> trace) {
        this.trace = trace;
    }
}
