请你编写一个程序来计算两个日期之间隔了多少天。

日期以字符串形式给出，格式为 YYYY-MM-DD，如示例所示。

 

示例 1：

输入：date1 = "2019-06-29", date2 = "2019-06-30"
输出：1
示例 2：

输入：date1 = "2020-01-15", date2 = "2019-12-31"
输出：15
 

提示：

给定的日期是 1971 年到 2100 年之间的有效日期。

class Solution {
    public int daysBetweenDates(String date1, String date2) {
        String[] d1 = date1.split("-");
        String[] d2 = date2.split("-");

        //我们只要计算从 1971 年 1 月 1 日到今天存在多少天数即可
        int days1 = (getInt(d1[0]) - 1971) * 365 + getMonthDays(getInt(d1[1])) + getInt(d1[2]); 
        int days2 = (getInt(d2[0]) - 1971) * 365 + getMonthDays(getInt(d2[1])) + getInt(d2[2]); 

        //加上闰年多的一天
        days1 += leapYear(getInt(d1[0]) - 1);
        days2 += leapYear(getInt(d2[0]) - 1);

        //如果今年是闰年，并且月份超过 2，那么将闰月的一天加回来
        if(isLeapYear(getInt(d1[0])) && getInt(d1[1]) > 2){
            days1++;
        }
        if(isLeapYear(getInt(d2[0])) && getInt(d2[1]) > 2){
            days2++;
        }
        return Math.abs(days1 -days2);
    }

    private int getInt(String str){
        return Integer.valueOf(str);
    }
    int[] months = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    //获取月份天数
    private int getMonthDays(int month){
        int days = 0;
        for(int i = 1; i < month; i++){
            days += months[i];
        }
        return days;
    }

    //计算存在多少个闰年
    private int leapYear(int y){
        int count = 0;
        while(y > 1971){
            if(isLeapYear(y--)){
                count++;
            }
        }
        return count;
    }
    //判断是否是闰年
    private boolean isLeapYear(int year){
        return year % 4 == 0 && year % 100 != 0 || year % 400 == 0;
    }
}