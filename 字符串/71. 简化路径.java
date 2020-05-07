
以 Unix 风格给出一个文件的绝对路径，你需要简化它。或者换句话说，将其转换为规范路径。

在 Unix 风格的文件系统中，一个点（.）表示当前目录本身；此外，两个点 （..） 表示将目录切换到上一级（指向父目录）；两者都可以是复杂相对路径的组成部分。更多信息请参阅：Linux / Unix中的绝对路径 vs 相对路径

请注意，返回的规范路径必须始终以斜杠 / 开头，并且两个目录名之间必须只有一个斜杠 /。最后一个目录名（如果存在）不能以 / 结尾。此外，规范路径必须是表示绝对路径的最短字符串。

 

示例 1：

输入："/home/"
输出："/home"
解释：注意，最后一个目录名后面没有斜杠。
示例 2：

输入："/../"
输出："/"
解释：从根目录向上一级是不可行的，因为根是你可以到达的最高级。
示例 3：

输入："/home//foo/"
输出："/home/foo"
解释：在规范路径中，多个连续斜杠需要用一个斜杠替换。

示例 4：

输入："/a/./b/../../c/"
输出："/c"
示例 5：

输入："/a/../../b/../c//.//"
输出："/c"
示例 6：

输入："/a//b////c/d//././/.."
输出："/a/b/c"


class Solution {
    public String simplifyPath(String path) {
		//按 / 进行分割
        String[] paths = path.split("/");
 
        Deque<String> stack = new LinkedList<>();
        for(String pa : paths){
			/*
			如果是 /. 表示当前路径，那么这里 pa = "." ，那么直接跳过
			如果是 // 需要去掉一个 /，那么通过分割后就是 ""，那么直接跳过
			*/
            if(".".equals(pa) || "".equals(pa)){
                continue;
            }
			/*
			如果是 //. 表示上一级目录，那么 pa = ".." ，如果栈不为空那么弹出上一个目录
			注意：栈是可能为空的，比如 path = "/../" ，那么这时栈是为空的
			*/
            if("..".equals(pa)){
                if(!stack.isEmpty()){
                    stack.pop();
                }
                continue;
            }
			//其他情况直接压栈
            stack.push(pa);
        }
		//如果栈为空，表示只需要返回根目录 "/"
        if(stack.isEmpty()){
            return "/";
        }
		//因为栈中元素是逆序的，因此我们先存放进 list 然后逆序添加
        List<String> res = new ArrayList<>();
        while(!stack.isEmpty()){
            res.add(stack.pop());
        }
        StringBuilder sb = new StringBuilder();
        for(int i = res.size() - 1; i >= 0; i--){
            sb.append("/").append(res.get(i));
        }
        return sb.toString();

    }
}