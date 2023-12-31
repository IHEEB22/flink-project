/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.playgrounds.spendreport;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.Tumble;
import org.apache.flink.table.expressions.TimeIntervalUnit;

import static org.apache.flink.table.api.Expressions.*;

public class SpendReport {
    public static Table report(Table health_status) {
        return health_status.window(Tumble.over(lit(1).hours()).on($("transaction_time")).as("w"))
                .groupBy($("patient_id"), $("gender"))
                .select(
                        $("patient_id"),
                        $("gender"),
                        $("Cholesterol ").avg().as("avg_Cholesterol "));
    }

    public static void main(String[] args) throws Exception {
        EnvironmentSettings settings = EnvironmentSettings.inStreamingMode();
        TableEnvironment tEnv = TableEnvironment.create(settings);

        tEnv.executeSql("CREATE TABLE health_status (\n" +
                "    patient_id  BIGINT,\n" +
                "    isSmoker    BIGINT,\n" +
                "    age       BIGINT,\n" +
                "    gender       BIGINT,\n" +
                "    Cholesterol        BIGINT,\n" +
                //    "    avg_Cholesterol        BIGINT,\n" +
                "    transaction_time TIMESTAMP(3),\n" +
                "    WATERMARK FOR transaction_time AS transaction_time - INTERVAL '5' SECOND\n" +
                ") WITH (\n" +
                "    'connector' = 'kafka',\n" +
                "    'topic'     = 'health_status',\n" +
                "    'properties.bootstrap.servers' = 'kafka:9092',\n" +
                "    'scan.startup.mode' = 'earliest-offset',\n" +
                "    'format'    = 'csv'\n" +
                ")");

        tEnv.executeSql("CREATE TABLE health_report (\n" +
                "    patient_id BIGINT,\n" +
                "    gender BIGINT,\n" +
                "    start_time TIMESTAMP(3),\n" +
                "    end_time TIMESTAMP(3),\n" +
                "    avg_Cholesterol  BIGINT,\n" +
                "    PRIMARY KEY (patient_id, start_time) NOT ENFORCED" +
                ") WITH (\n" +
                "  'connector'  = 'jdbc',\n" +
                "  'url'        = 'jdbc:mysql://mysql:3306/sql-demo',\n" +
                "  'table-name' = 'health_report',\n" +
                "  'driver'     = 'com.mysql.jdbc.Driver',\n" +
                "  'username'   = 'sql-demo',\n" +
                "  'password'   = 'demo-sql'\n" +
                ")");

        Table transactions = tEnv.from("health_status");
        report(transactions).executeInsert("health_report");
    }
}
