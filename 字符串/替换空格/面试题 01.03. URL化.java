URL化。编写一种方法，将字符串中的空格全部替换为%20。假定该字符串尾部有足够的空间存放新增字符，并且知道字符串的“真实”长度。（注：用Java实现的话，请使用字符数组实现，以便直接在数组上操作。）

示例1:

 输入："Mr John Smith    ", 13
 输出："Mr%20John%20Smith"
示例2:

 输入："               ", 5
 输出："%20%20%20%20%20"

class Solution {
    public String replaceSpaces(String S, int length) {
        //按题目要求，在除开所有字母占有的长度外，剩下的长度是给空格的，并且是给排在前面的空格的
        StringBuilder sb = new StringBuilder();
        for(char ch : S.toCharArray()){
            if(length <= 0){
                break;
            }
            //遇到空格，直接添加 %20，遇到字母，直接添加字母
            if(ch == ' '){
                sb.append("%20");
            }else{
                sb.append(ch);
            }
            length --;
        }
        return sb.toString();
    }
}