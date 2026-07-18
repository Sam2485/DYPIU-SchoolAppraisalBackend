import subprocess

query = (
    "UPDATE submissions "
    "SET status = 'DRAFT', "
    "    submitted_at = NULL, "
    "    values_data = (jsonb_set(values_data::jsonb - '__administrativeSubmissionStatus', "
    "                             '{administrativeProgress}', "
    "                             '{\"registrar\":\"DRAFT\",\"hr\":\"DRAFT\",\"dean-student-welfare\":\"DRAFT\",\"dean-placement\":\"DRAFT\"}'::jsonb))::text "
    "WHERE audit_type = 'administrative' AND version = 2;"
)

print("Executing PostgreSQL update via python subprocess...")
res = subprocess.run(["sudo", "-u", "postgres", "psql", "-d", "school_appraisal_db", "-c", query], capture_output=True, text=True)
print("STDOUT:", res.stdout)
print("STDERR:", res.stderr)
if res.returncode == 0:
    print("Success!")
else:
    print("Failed with return code:", res.returncode)
