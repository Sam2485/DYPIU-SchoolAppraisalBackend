import subprocess

query = "SELECT id, email, school, status, report_category, version, academic_year FROM submissions ORDER BY email, version, id;"

print("Executing psql query to list all submissions...")
res = subprocess.run(["sudo", "-u", "postgres", "psql", "-d", "school_appraisal_db", "-c", query], capture_output=True, text=True)
print("STDOUT:")
print(res.stdout)
