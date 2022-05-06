package com.pukka.iptv.downloader.policy;

/**
 * "多劳多得平均均匀分配"策略
 */
public class WorkMoreGetMoreStrategy {

    public static void main(String[] args) {
        // 下载节点数及每个下载节点数剩余任务数量
        int[] arr = {5,5,3,10,1,1,7};
        //int[] arr = {10,10,10,10,10,10};
        //int[] arr = {0,0,0,0,0,0,0};
        //待分配任务数
        int task = 301;

        //两种临界情况，都为0或都为10
        boolean flag = check(arr);
        if(flag) {

            // 如果全为0，表示需要平均分配； 否则全为10，不需要添加
            boolean isAdd = arr[0] == 0 ? true:false;
            if(isAdd) {
                //平均分配
                averageDistribution(arr, task, (arr.length - 1), (task/arr.length > 10 ? 10 : task/arr.length));
                task = 0;
            }

            //打印数组
            print(arr, task);
        } else {

            //排序
            sort(arr);

            //处理
            handle(arr, task, 0);
        }
    }

    /**
     * 校验两种情况
     * @param arr
     */
    private static boolean check(int[] arr) {
        int count = 0;
        for (int i = 0; i < arr.length; i++) {
            if(arr[i] == 0 || arr[i] == 10) {
                count++;
            }
        }
        return count == arr.length;
    }

    /**
     *
     * @param arr
     * @param task
     * @param num 记录最小的元素索引为几,比如(3,3,3,5)此时num索引为2
     */
    private static void handle(int[] arr, int task, int num) {
        /*if((num+1) >= (arr.length - 1)) {
            return;
        };*/
        if(num == (arr.length-1) && task > 0) {
            averageDistribution(arr, task, (arr.length - 1), (10-arr[arr.length -1 ]));
            //打印数组
            print(arr, 0);
            return;
        }
        int diff = (arr[num+1] - arr[num]);
        int numValue = getNumValue(arr, (num + 1));
        if(task <= (diff*(num+1))) {
            // 平均分配
            System.out.println("平均分配");
            averageDistribution(arr, task, num, task/arr.length);
            //打印数组
            print(arr, 0);
            return;
        } else {
            for(int i = 0; i < num+1; i++) {
                // 补差值
                arr[i] = arr[num+1];
            }
            // 同步待分配任务数
            task = task - (diff*(num + 1));
            //打印数组
            print(arr, task);
            // 递归执行
            handle(arr, task, (num+ numValue));
        }
    }

    /**
     * 排序
     * @param arr
     */
    private static void sort(int[] arr) {
        if(arr != null && arr.length > 0) {
            int tem = 0;
            for (int i = 0; i < arr.length; i++) {
                for(int j = i+1; j < arr.length; j++) {
                    if(arr[i] > arr[j]) {
                        tem = arr[i];
                        arr[i] = arr[j];
                        arr[j] = tem;
                    }
                }
            }
        }
    }

    /**
     * 确定
     * @param arr
     * @param i
     * @return
     */
    private static int getNumValue(int[] arr, int i) {
        int num = 1;
        for (int j = i+1; j < arr.length; j++) {
            if(arr[i] == arr[j]) {
                num++;
            } else {
                break;
            }
        }
        return num;
    }

    /**
     * 平均分配
     * @param arr 数组
     * @param task 可分配的任务数量
     * @param num 记录最小的元素索引为几,比如(3,3,3,5)此时num索引为2
     * @param diff 差值
     */
    private static void averageDistribution(int[] arr, int task,int num, int diff) {
        if(task >= (num+1)) {
            // 平均分配
            // int result = task/num;
            for (int i = 0; i < (num+1); i++) {
                arr[i] += diff;
            }
            if(task <= (diff*(num+1))) {
                int result =  task%(num+1);
                for (int i = 0; i < result; i++) {
                    arr[i]++;
                }
            }
        } else {
            for (int i = 0; i < task; i++) {
                arr[i]++;
            }
        }
    }

    /**
     * 打印数组
     */
    private static void print(int[] arr, int task) {
        System.out.print("Begin------------[");
        for (int i = 0; i < arr.length; i++) {
            if (i == (arr.length - 1)) {
                System.out.print(arr[i]);
            } else {
                System.out.print(arr[i] + ",");
            }
        }
        System.out.print("]------------End, 待分配任务数:"+task);
        System.out.println();
    }
}
