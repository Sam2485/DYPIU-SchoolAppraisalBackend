import subprocess

query = "SELECT id, email, school, status, report_category, version, academic_year, audit_cycle FROM submissions WHERE email = 'arvind.kumar@dypiu.ac.in';"

print("Executing psql query on VM database...")
res = subprocess.run(["sudo", "-u", "postgres", "psql", "-d", "school_appraisal_db", "-c", query], capture_output=True, text=True)
print("STDOUT:")
print(res.stdout)
print("STDERR:")
print(res.stderr)
