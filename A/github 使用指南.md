# github 使用指南



### 1、git init 初始化本地仓库

```shell
git init
```

它会在当前目录下创建一个 `.git ` 的文件夹，将当前文件夹作为本地 git 仓库，项目文件可以存储在这个本地仓库中，后续用于提交到 github 远程仓库

`.git` 文件夹默认是隐藏的，可以通过 `ls -a` 查看

此阶段下仅仅只是一个本地 git 仓库，没有关联任何的一个 github 账号和 github 项目



### 2、git 分区

首先我们需要知道 git 的 3 个分区：工作区、暂存区、版本库

假设我们在 /oy 目录下执行了 `git init`，那么就是将 oy 目录作为本地仓库，它会在 oy 目录下创建一个 `.git` 文件夹

*![image.png](https://pic.leetcode-cn.com/1613890831-ziVfvI-image.png)*

在 /oy 目录下，除了 `.git` 文件夹外，其他的都是工作区，`.git` 叫做版本库

在 `.git` 中，有一个 `index` 文件，它是暂存区，`git add` 的文件都会存储在这个暂存区中

我们可以撤销掉暂存区中的文件，不进行提交

当我们执行 `git commit ` ，就会将暂存区所有的文件都提交到版本库上，即远程 github 仓库上

```
工作区和暂存区、版本库的文件仅对于自己可见，当我们 push 提交到远程仓库，那么文件对于其他成员可见
```



### 3、git remote 本地仓库关联 github 远程仓库

```shell
git remote add [本地仓库名] [github 链接]

例子：
git remote add origin \
https://github.com/1471104698/leetcode.git
```

remote：远程

该命令用于将当前本地 git 仓库关联到远程 github 仓库

上面我们将远程仓库和本地仓库进行关联，同时将远程仓库命名为 `origin`，这个名字可以自定义，后面跟着的是远程仓库的 url





### 4、git add 和 git commit

```shell
git add [文件名]
git commit -m "提交信息"
```

git add 是将工作区的文件提交到暂存区

git commit 是将暂存区的文件提交到版本库，此时的版本库并非是远程仓库，而是我们确定了修改 commit 了的文件，无法进行撤销，只能进行版本回退



`git add [文件名]`：添加某个文件

`git add -A`：添加当前目录的所有文件

`git add .`：添加当前目录修改过的文件



### 5、git status 状态检查

```shell
git status
```

通过 `git status` 可以查看当前 `.git` 本地仓库的状态，比如暂存区存在什么文件，哪些文件是新文件，哪些文件是修改过未 commit 的文件

比如下面

在当前暂存区中，存在一个新文件：`o.txt`

在 master 分支中，存在一个修改过但是未 add 到暂存区的文件：`oy.txt`

<img src="https://pic.leetcode-cn.com/1613891689-IuJgvy-image.png" style="zoom:80%;" />



### 6、git 分支（branch）

github 上是存在不同的分支的，一个项目可以存在不同的分支，每个分支上的代码可以是不同的

一般情况下，master 分支是项目的主分支，当我们执行 `git init` 的时候，它默认会在本地创建一个 master 分支

当一个团队对于项目某部分代码存在不确定的情况的时候，可以另外建立一个分支，在统一完成后，再将其他分支的代码合并到 master 分支上

<img src="https://pic.leetcode-cn.com/1613891386-kAQhRh-image.png" style="zoom:70%;" />

1、git branch 查看所有的分支

```shell
git branch

git branch -a				//查看所有本地分支和远程分支
git branch -a --merged		//查看所有本地分支和远程分支中已经合并的分支
git branch -a --no-merged	//查看所有本地分支和远程分支中已经合并的分支
git branch 		//查看本地分支
git branch -r 	//查看远程分支
```

`git branch` 可以用来查看当前 git 下存在什么分支

比如下面，git 下存在两个分支：main 和 master，同时 main 是主分支

*![image.png](https://pic.leetcode-cn.com/1613892625-RGoKbA-image.png)*



2、git checkout 切换分支

```shell
git checkout [分支名]

例子：
git checkout master
```

当我们存在多个分支时，可以使用 git checkout 切换到我们要进行修改的分支



3、git push 推送本地分支到远程仓库

```shell
git push [远程仓库名] [分支名]

例子
git push origin main
```

当我们本地仓库中创建了一个新的分支，或者本地分支上存在新文件或者已经修改的文件，那么我们可以使用 `git push` 将本地仓库的分支以及本地分支中的文件推送到远程仓库上

一般我们执行的顺序为 git add -> git commit -> git push



4、git merge 合并分支

```shell
git merge [分支名]

例子
git checkout master
git merge main
```

一旦存在子分支，就是因为我们需要将某部分代码给分离出来单独处理，最终还是需要合并到主项目上，因此需要进行分支合并

我们可以使用 `git merge` 将任意一个分支合并到当前分支上

上面的例子就是先切换到 master 分支，然后将 main 分支上的文件合并到 master 分支上去



5、git branch -d/D 分支删除

```shell
git branch -d [分支名] //不会删除未合并的分支
git branch -D [分支名] //会删除未合并的分支
```



6、重命名本地分支

```shell
git branch -m [旧分支名] [新分支名]

例子
git branch -m main main1111
```

修改后的本地分支如果 push 后远程仓库没有该分支，那么在远程仓库会重新创建一个新的分支，

不会影响到 旧分支名 所在的分支



7、git log 查看历史版本

```shell
git log	//查看当前分支的所有历史版本
git log -x //查看当前分支最新的 x 个版本
git log -x [文件名] //查看当前分支的某个文件最新的 x 个版本，需要进入该文件所在目录
```



8、git reset 回滚版本

```shell
git reset HEAD			//回滚掉暂存区的记录，即回滚掉 git add 操作
git reset HEAD^	//回滚掉 git commit 操作，当前分支回滚到上个版本
git reset HEAD^~2	//当前分支回滚到前两个版本
git reset [版本号或版本号前几位]	//将当前分支 回滚到指定版本号，如果是版本号前几位，git会自动寻找匹配的版本号
git reset [版本号或版本号前几位] [文件名]	//将当前分支 某个文件回滚到指定版本号

git reset --hard HEAD^
```

加 --hard 和 不加的区别在于：

①、当我们没有添加 --hard 时，那么它的回滚的是在暂存区的，具体作用于工作区文件还需要我们执行 `git status` 进行查看并操作

比如下面，当我们执行了 git reset 后，执行 git status 它存在两个选择让我们进行操作，即 git reset 单单只是作用于暂存区，没有直接作用于工作区

<img src="https://pic.leetcode-cn.com/1613918845-sahPti-image.png" style="zoom:80%;" />



②、添加了 --hard 后，它会直接作用于工作区



### 7、git fetch / git pull 拉取远程仓库的分支

```shell
git fetch [远程仓库名] [远程分支名]

git pull [远程仓库名] [远程分支名] ：[本地分支名]
	如果是远程分支名和本地分支名相同，那么可以省略掉 ":"
git pull [远程仓库名] [远程分支名]
```

`git fetch` 和 `git pull` 拉取远程仓库上某个分支到本地仓库中

①、`git fetch` 拉取远程仓库中的文件到本地仓库中，不过不会直接合并到本地仓库的文件，而是存储在 FECT_HEAD 中（`.git`中存在一个 FETCH_HEAD 文件，它记录 fetch）

②、在 `git fetch` 完成后，可以执行 `git log-p FETCH_HEAD` 来查看拉取的日志（红色的是本地文件应该删除的，绿色的是本地文件应该增加的）

<img src="https://pic.leetcode-cn.com/1613917462-rAySMs-image.png" style="zoom:90%;" />

③、检查完 log 后，再执行 `git merge FETCH_HEAD` 将拉取的新改动进行合并



`git pull` 是拉取后直接合并，即 `git pull = git fetch + git merge`，需要后续自己处理冲突



### 8、git 合并冲突