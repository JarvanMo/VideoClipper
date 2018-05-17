package com.jarvanmo.videoclipper.util;

import android.os.AsyncTask;
import android.os.Build;

public class AsyncTaskCompat {
    /**
     * Executes the task with the specified parameters, allowing multiple tasks to run in parallel
     * on a pool of threads managed by {@link android.os.AsyncTask}.
     *
     * @param task The {@link android.os.AsyncTask} to execute.
     * @param params The parameters of the task.
     * @return the instance of AsyncTask.
     */
    public static <Params, Progress, Result> AsyncTask<Params, Progress, Result> executeParallel(
            AsyncTask<Params, Progress, Result> task, Params... params) {
        if (task == null) {
            throw new IllegalArgumentException("task can not be null");
        }

            // Before API 11, all tasks were run in parallel
            task.execute(params);

        return task;
    }
}
