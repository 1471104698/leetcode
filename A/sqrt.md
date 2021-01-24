# sqrt

```java
    private void sqrt(int n) {
        double eps = 0.0001;
        double left = 0;
        double right = n;
        while (right - left > eps) {
            double mid = (left + right) / 2;
            //这里不能直接 + 1 了
            if (mid * mid < n) {
                left = mid;
            } else {
                right = mid;
            }
        }
        //0.001
        int p = (int) left;
        if (left + 0.001 >= p + 1) {//比如 n = 9，此时计算出来的 left = 2.999xxx，此时 left + 0.001 = 3.000xxx，比 p + 1 = 3 还大，所以直接转换为 3
            left = p + 1;
        }
        if (left - 0.001 <= p) {    //比如 left = 2.000xxxx，此时 left - 0.001 = 1.999xxx，比 p = 2 还小，那么直接取整 left = p = 2
            left = p;
        }
        System.out.println(left);
        System.out.println(Math.sqrt(n));
    }
```

