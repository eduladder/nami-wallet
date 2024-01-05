--changeset p.surve:1
CREATE TABLE heap_summary (
  heap_summary_id SERIAL PRIMARY KEY,
  heap_id		INTEGER NOT null UNIQUE,
  used_heap_size		INTEGER NOT null,
  class_count  	INTEGER not null,
  object_count  	INTEGER not null,
  class_loader_count  	INTEGER not null, 
  gc_root_count  	INTEGER not null,
  os_bit		VARCHAR(50) not null,
  pod		VARCHAR(5) not null,
  host_name		VARCHAR(20) not null,
  jvm_parameters JSON not null,
  heap_creation_date TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  
);

CREATE TABLE dominator_tree (
  dominator_tree_id SERIAL PRIMARY KEY,
  heap_id		INTEGER NOT null,
  FOREIGN KEY (heap_id) REFERENCES heap_summary (heap_id),
  object_id		INTEGER NOT null,
  parent_id  	INTEGER null,
  object_label	VARCHAR(500) NOT null,
  memory_Location VARCHAR(50) NOT null,
  origin		BOOLEAN DEFAULT false,
  prefix		VARCHAR(50) null,
  suffix		VARCHAR(50) null,
  shallow_size	INTEGER NOT null,
  retained_size BIGINT NOT null,
  has_inbound	BOOLEAN DEFAULT false,
  has_outbound	BOOLEAN DEFAULT false,
  object_type	SMALLINT NOT null,
  gc_root		BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE histogram (
  histogram_id SERIAL PRIMARY KEY,
  heap_id		INTEGER NOT null,
  FOREIGN KEY (heap_id) REFERENCES heap_summary (heap_id),
  object_id		INTEGER NOT null,
  object_label	VARCHAR(500) NOT null,
  number_of_objects	SMALLINT NOT null,
  object_type	SMALLINT NOT null,
  shallow_size	INTEGER NOT null,
  retained_size BIGINT NOT null,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE histogram_reference (
  histogram_reference_id SERIAL PRIMARY KEY,
  histogram_id		INTEGER NOT null,
  FOREIGN KEY (histogram_id) REFERENCES histogram (histogram_id),
  object_id		INTEGER NOT null,
  object_label	VARCHAR(500) NOT null,
  object_type	SMALLINT NOT null,
  is_inbound	BOOLEAN DEFAULT false,
  has_inbound	BOOLEAN DEFAULT false,
  has_outbound	BOOLEAN DEFAULT false,
  shallow_size	INTEGER NOT null,
  retained_size BIGINT NOT null,
  gc_root		BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE thread_info (
  thread_info_id SERIAL PRIMARY KEY,
  heap_id		INTEGER NOT null,
  FOREIGN KEY (heap_id) REFERENCES heap_summary (heap_id),
  object_id		INTEGER NOT null,
  object_label	VARCHAR(500) NOT null,
  thread_name	VARCHAR(500) NOT null,
  context_class_loader VARCHAR(500) NOT null,
  has_stack	BOOLEAN DEFAULT false,
  is_daemon	BOOLEAN DEFAULT false,
  shallow_size	INTEGER NOT null,
  retained_size BIGINT NOT null,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE thread_stack (
  thread_stack_id SERIAL PRIMARY KEY,
  thread_info_id		INTEGER NOT null,
  FOREIGN KEY (thread_info_id) REFERENCES thread_info (thread_info_id),
  stack	VARCHAR(500) NOT null,
  has_local	BOOLEAN DEFAULT false,
  first_non_native_frame BOOLEAN DEFAULT false,
  object_id		INTEGER NOT null,
  object_label	VARCHAR(500) NOT null,
  prefix		VARCHAR(50) null,
  suffix		VARCHAR(50) null,
  has_inbound	BOOLEAN DEFAULT false,
  has_outbound	BOOLEAN DEFAULT false,
  shallow_size	INTEGER NOT null,
  retained_size BIGINT NOT null,
  gc_root		BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);