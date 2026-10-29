# Java错题案例与提示词对照实验

## 30题正式实验与双人复核

　　开题报告规定的正式实验是30道Java错题：编译错误、运行错误和逻辑错误各10道。`five_conditions.py`要求每题提供题意、错误代码、报错信息、实际输出、预期输出、参考代码、原因说明、可执行测试、来源和人工核验人。`verified_by`不能填写`pending`。现有150条是候选来源，不能自动等同于正式30题。

　　(1) 先由人工核实30题标准答案，离线编译参考代码并运行对应测试。将合格案例按脚本字段写入`formal_30.jsonl`。在隔离环境运行模型对照；每题5种输入条件各重复2次，目标共300次。生成代码只保存在记录中，脚本不会执行它。

```powershell
python experiments/five_conditions.py run --manifest experiments/formal_30.jsonl --formal --max-cases 30 --output experiments/formal_runs.jsonl
python experiments/five_conditions.py blind --runs experiments/formal_runs.jsonl --output experiments/formal_review
```

　　(2) 实验管理员保留`PRIVATE_mapping.json`。两名具备Java基础的评分者分别拿到`review_outputs.json`、标准答案和自己的空白评分表，独立填写0或1。原因、修改建议、知识点按标准答案和题目要求核对；`code_runs`只在隔离环境离线编译并通过该题测试后填1。无法判断或没有实际执行时留空，并在`notes`写明原因。两名评分者不能互看评分表，系统普通用户账号也不承载专家评分；此处使用独立CSV。

　　(3) 先汇总各指标的双人同时评分数、一致数、一致率及缺失数，再逐条处理分歧。运行下面命令会生成`summary.json`和`disagreements.csv`。在分歧表填入最终0/1结论和复核依据后，再运行带`--adjudicated`的命令。未完成的评分不会被当成0分，也不会生成虚假的正确率。

```powershell
python experiments/five_conditions.py summarize --runs experiments/formal_runs.jsonl --mapping experiments/formal_review/PRIVATE_mapping.json --reviewer-a experiments/formal_review/reviewer_A.csv --reviewer-b experiments/formal_review/reviewer_B.csv --output-dir experiments/formal_summary
python experiments/five_conditions.py summarize --runs experiments/formal_runs.jsonl --mapping experiments/formal_review/PRIVATE_mapping.json --reviewer-a experiments/formal_review/reviewer_A.csv --reviewer-b experiments/formal_review/reviewer_B.csv --output-dir experiments/formal_summary --adjudicated experiments/formal_summary/disagreements.csv
```

　　报告结构完整性时，分母是全部模型请求，失败请求另列；报告人工正确性时，分母是两人均完成评分的有效输出。评分者应记录模型版本、温度0.2、运行日期、设备和每次请求耗时。该样本不能证明真实课堂学习效果。

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
