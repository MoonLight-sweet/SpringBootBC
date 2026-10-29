import csv
import json
import tempfile
import unittest
from pathlib import Path
from types import SimpleNamespace

import five_conditions


class SummaryTest(unittest.TestCase):
    def test_reports_agreement_before_adjudication_and_requires_reason(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            runs = [
                {"case_id": "c1", "condition": "S1", "repeat": 1, "error": None, "response": "{}", "structure_valid": True},
                {"case_id": "c1", "condition": "S2", "repeat": 1, "error": None, "response": "text", "structure_valid": False},
            ]
            mapping = [
                {"blind_id": "B0001", "case_id": "c1", "condition": "S1", "repeat": 1},
                {"blind_id": "B0002", "case_id": "c1", "condition": "S2", "repeat": 1},
            ]
            run_path, map_path = root / "runs.jsonl", root / "mapping.json"
            run_path.write_text("\n".join(json.dumps(r) for r in runs), encoding="utf-8")
            map_path.write_text(json.dumps(mapping), encoding="utf-8")
            fields = ["blind_id", *five_conditions.SCORE_FIELDS]
            for reviewer, values in (("A", ["1", "0"]), ("B", ["0", "0"])):
                with (root / f"reviewer_{reviewer}.csv").open("w", newline="", encoding="utf-8") as file:
                    writer = csv.DictWriter(file, fieldnames=fields)
                    writer.writeheader()
                    writer.writerows({"blind_id": f"B{i:04d}", **{f: values[i - 1] for f in five_conditions.SCORE_FIELDS}}
                                     for i in (1, 2))
            args = SimpleNamespace(runs=run_path, mapping=map_path, reviewer_a=root / "reviewer_A.csv",
                                   reviewer_b=root / "reviewer_B.csv", output_dir=root / "summary", adjudicated=None)
            five_conditions.summarize(args)
            result = json.loads((root / "summary" / "summary.json").read_text(encoding="utf-8"))
            self.assertEqual(result["agreement_before_review"]["root_cause_correct"]["agreement_rate"], 0.5)
            self.assertEqual(result["disagreement_cells"], 4)
            self.assertNotIn("after_review", result)
            with (root / "summary" / "disagreements.csv").open(encoding="utf-8-sig", newline="") as file:
                rows = list(csv.DictReader(file))
            for row in rows:
                row["adjudicated"] = "1"
                row["reason"] = "对照标准答案和离线测试记录"
            with (root / "adjudicated.csv").open("w", encoding="utf-8-sig", newline="") as file:
                writer = csv.DictWriter(file, fieldnames=rows[0].keys())
                writer.writeheader()
                writer.writerows(rows)
            args.adjudicated = root / "adjudicated.csv"
            five_conditions.summarize(args)
            final = json.loads((root / "summary" / "summary.json").read_text(encoding="utf-8"))
            self.assertEqual(final["after_review"]["root_cause_correct"]["S1"], {"scored": 1, "correct": 1})


if __name__ == "__main__":
    unittest.main()
