#!/usr/bin/env bash

# Default values
recordcount=""
threads=""
counts_naming=""
cpu_count=""

# Function to check if parameters are set
check_param() {
    local param_value="$1"
    local flag="$2"
    local desc="$3"

    if [ -z "${param_value}" ]; then
        echo "Please provide ${desc} using the -${flag} flag."
        exit 1
    fi
}

# Parse command line arguments
while getopts r:t:n:c: flag; do
    case "${flag}" in
        r) recordcount=${OPTARG};;
        t) threads=${OPTARG};;
        n) counts_naming=${OPTARG};;
        c) cpu_count=${OPTARG};;
    esac
done

# Check if the required parameters are provided
check_param "${recordcount}" "r" "a record count"
check_param "${threads}" "t" "the thread count"
check_param "${counts_naming}" "n" "the naming value of record counts"
check_param "${cpu_count}" "c" "the CPU units value"

# Display provided values
cat <<EOF
Provided values:
Record count: ${recordcount}
Thread count: ${threads}
Record count naming: ${counts_naming}
CPU units: ${cpu_count}

Finished reading properties...
**************************************
Start loading entries to database...
EOF

# Function to run YCSB tests for different plans
run_ycsb() {
    local phase="$1"
    local workload="$2"
    local suffix="$3"
    ../bin/ycsb.sh run cloudspanner -P ../conf/cloudspanner.properties -P "../workloads/${workload}" -p recordcount=${recordcount} -p operationcount=1000000 -p measurementtype=timeseries -threads ${threads} -s > "../out/run-${counts_naming}-${cpu_count}-${threads}t-${suffix}.csv"
    echo -e "\nFinished tests on ${phase}..\n"
}

# Load Phase
#../bin/ycsb.sh load cloudspanner -P ../conf/cloudspanner.properties -P ../workloads/workloada -p recordcount=${recordcount} -p cloudspanner.batchinserts=1000 -p cloudspanner.insertorder=ordered -p measurementtype=timeseries -threads ${threads} -s > "../out/load-${counts_naming}-${cpu_count}-${threads}t.csv"
#echo -e "\nFinished loading entries to database..\n"

# Run Phases
echo -e "**************************************\nStart running test on Plan A...\n"
run_ycsb "Plan A" "workloada" "A"
echo -e "**************************************\nStart running test on Plan B...\n"
run_ycsb "Plan B" "workloadb" "B"
echo -e "**************************************\nStart running test on Plan F...\n"
run_ycsb "Plan F" "workloadf" "F"

echo -e "\nEnd of tests.."
