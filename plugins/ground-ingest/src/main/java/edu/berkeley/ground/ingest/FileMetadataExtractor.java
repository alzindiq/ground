package edu.berkeley.ground.ingest;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.Properties;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import com.google.common.collect.ImmutableList;
import gobblin.configuration.WorkUnitState;
import gobblin.source.extractor.filebased.FileBasedExtractor;
import gobblin.source.extractor.hadoop.AvroFsHelper;
import gobblin.configuration.ConfigurationKeys;


/**
 * An extractor that pulls out file metadata.
 */
public class FileMetadataExtractor extends FileBasedExtractor<Schema, GenericRecord> {

    private final String sourceDirectory;
    
    //creating the schema to store the file metadata
    private static final String SCHEMA_STRING = "{\"namespace\": \"ground.avro\", "
      + "\"type\": \"record\","
      + "\"name\": \"Metadata\", "
      + "\"fields\": [ "
      + "{\"name\": \"name\", \"type\": [\"string\", \"null\"]}, "
      + "{\"name\": \"timeCreated\", \"type\": [\"long\", \"null\"]},"
      + "{\"name\": \"length\", \"type\": [\"long\", \"null\"]},"
      + "{\"name\": \"modificationTime\", \"type\": [\"long\", \"null\"]},"
      + "{\"name\": \"owner\", \"type\": [\"string\", \"null\"]} "

      + "]"
      + "}";

    private static Schema OUTPUT_SCHEMA = new Schema.Parser().parse(SCHEMA_STRING);
    private final FileSystem fs;

    public FileMetadataExtractor(WorkUnitState workUnitState)
        throws IOException {
    super(workUnitState, new AvroFsHelper(workUnitState));
        Properties props = workUnitState.getProperties();
        Config config = ConfigFactory.parseProperties(props);
        this.sourceDirectory = config.getString(ConfigurationKeys.SOURCE_FILEBASED_DATA_DIRECTORY);
        fs = FileSystem.get(URI.create(sourceDirectory),new Configuration());
  }

    @Override
    public Iterator<GenericRecord> downloadFile(String file) throws IOException {

        Path path = new Path(file);
        FileStatus[] statuses = fs.listStatus(path);

        //creating the GenericRecord to store the metadata
        GenericRecord record = new GenericData.Record(OUTPUT_SCHEMA);
        record.put("name", statuses[0].getPath().toString());
        record.put("timeCreated", statuses[0].getAccessTime());
        record.put("length", statuses[0].getLen());
        record.put("modificationTime", statuses[0].getModificationTime());
        record.put("owner", statuses[0].getOwner().toString());

        return ImmutableList.of(record).iterator();
        
  }

    @Override
    public Schema getSchema() {
        return OUTPUT_SCHEMA;
  }

}
