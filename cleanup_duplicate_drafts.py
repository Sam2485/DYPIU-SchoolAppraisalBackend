import subprocess

query = "DELETE FROM submissions WHERE (school IS NULL OR school = '') AND status = 'DRAFT';"

print("Executing delete query on VM database...")
res = subprocess.run(["sudo", "-u", "postgres", "psql", "-d", "school_appraisal_db", "-c", query], capture_output=True, text=True)
print("STDOUT:")
print(res.stdout)
print("STDERR:")
print(res.stderr)
