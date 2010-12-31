DROP TABLE IF EXISTS dag_outputs;
CREATE TABLE dag_outputs (
  dag_id varchar(100) NOT NULL,
  contract_id varchar(100) NOT NULL,
  output_data LONGTEXT NOT NULL,
  PRIMARY KEY  (contract_id),
  INDEX (dag_id)
);

DROP TABLE IF EXISTS dag_contracts;
CREATE TABLE dag_contracts (
  dag_id varchar(100) NOT NULL,
  contract_id varchar(100) NOT NULL,
  num_restarts int(20) default '0',
  retry tinyint(1) default 0,
  last_updated timestamp,
  start_time datetime,
  next_retry_time datetime,
  description longtext,
  status varchar(100),
  contract longtext,
  PRIMARY KEY (contract_id),
  INDEX (dag_id)
);

DROP TABLE IF EXISTS dag_deps;
CREATE TABLE dag_deps (
  dag_id varchar(100) NOT NULL,
  contract_id varchar(100) NOT NULL,
  dependent_contract_id varchar(100) NOT NULL,
  dep_status tinyint(1) default 0,
  PRIMARY KEY  (contract_id,dependent_contract_id),
  INDEX(dag_id),
  INDEX(dependent_contract_id)
);

DROP TABLE IF EXISTS dag_logs;
CREATE TABLE dag_logs(
  dag_id varchar(100) NOT NULL,
  contract_id varchar(100) NOT NULL,
  log_time datetime,
  log_message longtext,
  error_reason longtext,
  detailed_description longtext,
  INDEX(dag_id),
  INDEX(contract_id)
);

DROP TABLE IF EXISTS dag_summary;
CREATE TABLE dag_summary (
  dag_id varchar(100) NOT NULL,
  start_time datetime,
  end_time datetime,
  status varchar(100) default 'InProgress',
  description text default NULL,
  PRIMARY KEY  (dag_id)
);

DROP TABLE IF EXISTS dag_contract_config;
CREATE TABLE dag_contract_config (
  dag_id varchar(100) NOT NULL,
  contract_id varchar(100) NOT NULL,
  config_key varchar(100) NOT NULL,
  config_value longtext NOT NULL,
  index(contract_id),
  index(dag_id)
);

