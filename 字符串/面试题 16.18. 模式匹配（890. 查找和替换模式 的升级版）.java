你有两个字符串，即pattern和value。 pattern字符串由字母"a"和"b"组成，用于描述字符串中的模式。
但需注意"a"和"b"不能同时表示相同的字符串。编写一个方法判断value字符串是否匹配pattern字符串。

//868 是 word 和 pattern 字符一一进行匹配，这里是 word 0 个、1 个 或 多个字符与 pattern 进行匹配，是 868 的升级版

示例 1：
输入： pattern = "abba", value = "dogcatcatdog"
输出： true

示例 2：
输入： pattern = "abba", value = "dogcatcatfish"
输出： false

示例 3：
输入： pattern = "aaaa", value = "dogcatcatdog"
输出： false

示例 4：
输入： pattern = "abba", value = "dogdogdogdog"
输出： true
解释： "a"="dogdog",b=""，反之也符合规则  //(这里说的是一个 pattern 字符可以匹配 0 个、1 个 或 多个 word 字符)
提示：

0 <= len(pattern) <= 1000
0 <= len(value) <= 1000
你可以假设pattern只包含字母"a"和"b"，value仅包含小写字母。
