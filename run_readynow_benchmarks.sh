# Each run is composed of these string identifiers:
#
# DATE - Date of experiment YYYYMMDD, by default obtained automatically
# BUILD - Base build ####  - zvm-dev-####, latest is teh default value
# PATCHES if any - any unsubmited pacthes, no_patches
# PARAMS - a text description of non-default parameters like C2CompilationThreashold
# JDKS - a version of jdk, jdk7 or jdk8
# FLAVOURS - only product by default
# EXPER - description of this run. reg_run - default value - just a normal periodic run

# Additional flags
# JAVA_OPTS_C2 - Replace c2 options with these
# JAVA_OPTS_FALCON - Replace falcon options with these
# JAVA_OPTS_COMMON - Replace common options with these (GC options for example)
# JAVA_BASE - path to the sandbox
# SPEC_PARAMS - Provide specjm params instead of default
# TYPES_OF_RUN - if not provided: all 4 combos of c2/doplin without/with profile
# BENCHES - if not provided - do compiler.compiler xml.validation
# BASE_OUTPUT_DIR - Needs to be writable. By default it is /home/ivan/readynow/perfdata

# Produced artifacts:
# Profile written - for all types of run
# Profile used - for "woth_profile" runs
# Readynow report file - output from  -XX:ProfileReportFile=
# Console log file - whatever was printed to console during the specjvm run
# File with Wiki tables - Produced by the supplied java tool
# SpecJVM reports - whatever specjvm generated


export JAVA_OPTS_DOLPHIN="-XX:+UseFalcon  ${JAVA_OPTS_COMMON} "
export JAVA_OPTS_C2=" ${JAVA_OPTS_COMMON} "

if [ -z "$DATE" ]; then
date=`date +%Y%m%d`
else
date=$DATE
fi
echo Date: ${date}

if [ -z "$EXPER" ]; then
export exper="reg_run"
else
exper=$EXPER
fi
echo Short description of the experiment: ${exper}

if [ -z "$JAVA_BASE" ]; then
  export java_base=/home/ivan/sandbox/azlinux/
else
  export java_base=$JAVA_BASE
fi
echo Will run java from following location: ${java_base}

if [ -z "$SPEC_PARAMS" ]; then
  export spec_params="-ikv -wt 5 -i 10 -bt 1 -it 60 --showversion -ict"
else
  export spec_params=${SPEC_PARAMS}
fi
echo Will run SpecJVM with these parameters: ${spec_params}

if [ -z "$JAVA_OPTS_COMMON" ]; then
  export java_opts_common="-showversion -XX:-DieOnSafepointTimeout -XX:+ShowMessageBoxOnError -XX:-SafepointWaitTimeProfiler  -Xmx1g -Xms1g -XX:-UseTickProfiler -XX:GenPauselessNewThreads=1 -XX:GenPauselessOldThreads=1  -XX:+PrintDeoptimizationStatistics -XX:+ProfilePrintReport"
else
  export java_opts_common=$JAVA_OPTS_COMMON
fi
echo Will run java with these flags: ${java_opts_common}

if [ -z "$JAVA_OPTS_C2" ]; then
  export java_opts_c2=" "
else
  export java_opts_c2=$JAVA_OPTS_COMMON
fi
echo C2 specific options: ${java_opts_c2}

if [ -z "$JAVA_OPTS_FALCON" ]; then
  export java_opts_falcon=" -XX:+UseFalcon "
else
  export java_opts_falcon=${JAVA_OPTS_FALCON}
fi
echo Falcon specific options: ${java_opts_falcon}

if [ -z "$TYPES_OF_RUN" ]; then
  export types_of_run="c2_without_profile c2_with_profile falcon_without_profile falcon_with_profile"
else
export types_of_run=$TYPES_OF_RUN
fi
echo Will run these types of tests: ${types_of_run}

if [ -z "$BENCHES" ]; then
  export benches="compiler.compiler xml.validation"
else
  export benches=$BENCHES
fi
echo Will run these benchmarks: ${benches}

if [ -z "$JDKS" ]; then
  export jdks="jdk8"
else
export jdks=$JDKS
fi
echo Will run these jdks: ${jdks}

if [ -z "$BUILD" ]; then
  export build="latest"
else
  export build=${BUILD}
fi
echo Build that we are using: ${build}

if [ -z "$PATCHES" ]; then
  export patches="no_patches"
else
  export patches=${PATCHES}
fi
echo patches to the build that we are using: ${patches}

if [ -z "$FLAVOURS" ]; then
  export flavours="product"
else
  export flavours=$FLAVOURS
fi
echo Will run these benchmark: ${flavours}

if [ -z "$INPUT_PROFILE" ]; then
  echo Will generate profile with c2_without_profile run and use it for all remaining runs
else
  echo Will use the user provided profile for all runs: $INPUT_PROFILE
fi

if [ -z "$BASE_OUTPUT_DIR" ]; then
  export base_output_dir=/home/ivan/readynow/perfdata
else
  export base_output_dir=$BASE_OUTPUT_DIR
fi
echo Output will be going to $base_output_dir

for bench in ${benches}; do

  for jdk in ${jdks}; do
  for flavour in ${flavours}; do

    unset ACCUMULATE_DESCRIPTIONS
    unset ACCUMULATE_DESCRIPTIONS_WITH_PROFILE

     #Make sure that no quotes are used - we need spaces in here
    for run in ${types_of_run}; do

      export output_dir=${base_output_dir}/${date}/${build}_${patches}/${exper}
      export file_suffix=${bench}.${run}.${jdk}.${flavour}.txt
      if [ "$run" = "c2_with_profile" ] ||  [ "$run" = "falcon_with_profile" ] ; then
        #Check that the input profile is available
        if [ "$INPUT_PROFILE" ]; then
          #if the shell variable INPUT_PROFILE is not empty
          if [ ! -f $INPUT_PROFILE ]; then
              echo "Variable INPUT_PROFILE is set but file with input profile not found!"
              continue
          fi
          export input_profile=$INPUT_PROFILE
        else
          #look for a profile generate with first iteration if this loop
          input_profile=${output_dir}/profile_out.${bench}.c2_without_profile.${jdk}.${flavour}.txt
          if [ ! -f $INP_PROFILE_FILE ]; then
              echo "Input profile not found and variable INPUT_PROFILE is not set!"
              continue
          fi
        fi
        export ACCUMULATE_DESCRIPTIONS_WITH_PROFILE="${ACCUMULATE_DESCRIPTIONS_WITH_PROFILE} ${run}"
      else
        export input_profile=temp_profile
        rm ${input_profile}
        if [ ! -f ${input_profile} ]; then
            echo Input profile is not found - that is how it should be
            touch ${input_profile}
        else
          echo Input profile is found - but that is not how it is suppose to be
          continue
        fi
      fi
      export output_profile=${output_dir}/profile_out.${file_suffix}

      mkdir -p ${output_dir}

      if [ ! -d "${output_dir}" ]; then
        echo can\'t create or read directory ${output_dir}
        continue
      fi

      export ACCUMULATE_DESCRIPTIONS="${ACCUMULATE_DESCRIPTIONS} ${run}"

      readynow_report_file=${output_dir}/readynow.${file_suffix}
      console_log_file=${output_dir}/console.${file_suffix}
      spec_results_path=${output_dir}/spec_results.${bench}.${run}

      if [ "$run" = "c2_without_profile" ] ||  [ "$run" = "c2_with_profile" ] ; then
        export java_opts="${java_opts_c2} ${java_opts_common} -XX:ProfileReportFile=${readynow_report_file}"
      fi

      if [ "$run" = "falcon_without_profile" ] ||  [ "$run" = "falcon_with_profile" ] ; then
        export java_opts="${java_opts_falcon} ${java_opts_common} -XX:ProfileReportFile=${readynow_report_file}"
      fi

      #Run SpecJVM
      export console_file=${output_dir}/console.${file_suffix}
      echo ${date} : Will now run ${run} run-specjvm.sh ${S} ${bench} using ${build}_${patches} ${flavour} | tee ${console_file}
      echo Path to java: ${JAVA_HOME} | tee -a ${console_file}
      export JAVA_OPTS="${java_opts} -XX:ProfileReportFile=${readynow_report_file} -XX:ProfileLogIn=${input_profile} -XX:ProfileLogOut=${output_profile} "
      export JAVA_HOME=${java_base}/${jdk}/x86_64/${flavour}
      sh run-specjvm.sh ${spec_params} ${bench} 2>&1 |  tee -a ${console_file}

      spec_output=`cat ${console_file} | awk 'f{print;f=0} /Generating reports in:/{f=1}'`
      mv ${spec_output} ${output_dir}/spec_results.${bench}.${run}

      if [ "$run" != "c2_without_profile" ] ; then
        cp ${input_profile} ${output_dir}/profile_in.${file_suffix}
      else
        touch ${output_dir}/profile_in.${file_suffix}
      fi
      if [ ! -f ${output_profile} ]; then
          echo Output profile not generated. Something went wrong
          touch ${output_profile}
      else
        continue
      fi
    done


    ${JAVA_HOME}/bin/java -jar ParseSpecResults.jar -t ${bench} \
    -d ${output_dir} -s ${ACCUMULATE_DESCRIPTIONS} \
    -f ${jdk}.${flavour}.txt  -r ${ACCUMULATE_DESCRIPTIONS_WITH_PROFILE} 2>&1 | tee wiki.${bench}.${jdk}.${flavour}.txt

  done
  done

done
echo "done" | xargs mail -r ivan@azul.com -q wiki.${bench}.${jdk}.${flavour}.txt -s "test ${date} ${exper} complete"  ivan@azul.com
