# Java错题案例与提示词对照实验

　　本目录保存150条候选案例的来源路径、源码散列值和300行人工评分表。候选案例来自[IntroClassJava官方仓库](https://github.com/Spirals-Team/IntroClassJava)的固定提交`b59f63e84611afc4249a9a4b0317600377c0c5a4`。仓库说明其含297个错误程序及测试，但本项目没有将“基准标记为错误程序”等同于本项目的人工核验结果。第三方Java源码保留在独立检出目录，不随本项目提交。

## 准备案例

　　(1) 在较短路径检出固定版本。Windows使用`core.longpaths=true`，因为原仓库有较长路径。

```powershell
git -c core.longpaths=true clone https://github.com/Spirals-Team/IntroClassJava.git D:\icj
git -C D:\icj checkout b59f63e84611afc4249a9a4b0317600377c0c5a4
python experiments/introclass.py manifest --dataset-root D:\icj
python experiments/introclass.py annotations
```

　　(2) 生成器按六种题型轮流取样，每个学生提交只取一个版本。每条记录包含源码路径和SHA-256，运行实验前会再次校验源码。当前150条分布为checksum7、digits33、grade33、median33、smallest32、syllables12；这些是候选样本，不是已人工核验的150条。

## 运行与标注

　　(1) 启动与应用相同的模型服务，再运行两种提示词。先用`--max-cases 2`检查输出，再运行全部150条。结果文件默认不会上传GitHub，避免把第三方源码、模型响应或潜在敏感内容放进提交。

```powershell
python experiments/introclass.py run --dataset-root D:\icj --max-cases 2 --output experiments/results_smoke.jsonl
python experiments/introclass.py run --dataset-root D:\icj --output experiments/results.jsonl
```

　　(2) 在`annotations.csv`中填写核验者和0/1评分：原因是否正确、修改建议是否正确、参考代码是否通过对应测试。参考代码运行需要在隔离的实验环境中按案例测试，不应在应用服务器内执行。未填写的单元格表示未核验，不得按0分或正确结果计算。

```powershell
python experiments/introclass.py summarize --runs experiments/results.jsonl --annotations experiments/annotations.csv
```

　　(3) 汇总会分别报告模型输出数、结构完整数和人工评分数。只有人工评分完整的行才进入正确率分母。该样本来自同一个公开基准，结果不能直接推广到真实课堂错题；公开基准也可能已进入模型训练材料。
