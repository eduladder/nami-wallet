package com.salesforce.cpp.heaphub.collect.models;

/**
 * A class to represent a pair of thread ids and their corresponding primary keys in sql database
 * To be used to store the stack traces of the threads in the database
 */
public class ThreadIds {
    public int thread_info_id;
    public int thread_id;

    /**
     * Constructor for ThreadIds
     * @param thread_info_id primary key id of thread info in sql database
     * @param thread_id the heap id for the thread
     */
    public ThreadIds(int thread_info_id, int thread_id) {
        this.thread_info_id = thread_info_id;
        this.thread_id = thread_id;
    }

}
