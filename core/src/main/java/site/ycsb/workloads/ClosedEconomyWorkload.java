/**
 * Copyright (c) 2010 Yahoo! Inc. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License. See accompanying LICENSE file.
 */

package site.ycsb.workloads;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import site.ycsb.ByteIterator;
import site.ycsb.Client;
import site.ycsb.DB;
import site.ycsb.DBException;
import site.ycsb.RandomByteIterator;
import site.ycsb.Status;
import site.ycsb.StringByteIterator;
import site.ycsb.Workload;
import site.ycsb.WorkloadException;
import site.ycsb.generator.ConstantIntegerGenerator;
import site.ycsb.generator.CounterGenerator;
import site.ycsb.generator.DiscreteGenerator;
import site.ycsb.generator.ExponentialGenerator;
import site.ycsb.generator.Generator;
import site.ycsb.generator.HistogramGenerator;
import site.ycsb.generator.HotspotIntegerGenerator;
import site.ycsb.generator.NumberGenerator;
import site.ycsb.generator.ScrambledZipfianGenerator;
import site.ycsb.generator.SkewedLatestGenerator;
import site.ycsb.generator.UniformLongGenerator;
import site.ycsb.generator.ZipfianGenerator;
import site.ycsb.measurements.Measurements;

/**
 * The core benchmark scenario. Represents a set of clients doing simple CRUD
 * operations. The relative proportion of different kinds of operations, and
 * other properties of the workload, are controlled by parameters specified at
 * runtime.
 *
 * Properties to control the client:
 * <UL>
 * <LI><b>fieldCount</b>: the number of fields in a record (default: 10)
 * <LI><b>fieldlength</b>: the size of each field (default: 100)
 * <LI><b>readAllFields</b>: should reads read all fields (true) or just one (false) (default: true)
 * <LI><b>writeAllFields</b>: should updates and read/modify/writes update all
 * fields (true) or just one (false) (default: false)
 * <LI><b>readproportion</b>: what proportion of operations should be reads (default: 0.95)
 * <LI><b>updateproportion</b>: what proportion of operations should be updates (default: 0.05)
 * <LI><b>insertproportion</b>: what proportion of operations should be inserts (default: 0)
 * <LI><b>scanproportion</b>: what proportion of operations should be scans (default: 0)
 * <LI><b>readmodifywriteproportion</b>: what proportion of operations should be
 * read a record, modify it, write it back (default: 0)
 * <LI><b>requestdistribution</b>: what distribution should be used to select
 * the records to operate on - uniform, zipfian, hotspot, or latest (default: uniform)
 * <LI><b>maxscanlength</b>: for scans, what is the maximum number of records to scan (default: 1000)
 * <LI><b>scanlengthdistribution</b>: for scans, what distribution should be
 * used to choose the number of records to scan, for each scan, between 1 and
 * maxscanlength (default: uniform)
 * <LI><b>insertorder</b>: should records be inserted in order by key
 * ("ordered"), or in hashed order ("hashed") (default: hashed)
 * </ul>
 */
public class ClosedEconomyWorkload extends Workload {

  /**
   * The name of the database table to run queries against.
   */
  public static final String TABLE_NAME_PROPERTY = "table";

  /**
   * The default name of the database table to run queries against.
   */
  public static final String TABLE_NAME_PROPERTY_DEFAULT = "usertable";
  /**
   * The name of the property for the number of fields in a record.
   */
  public static final String FIELD_COUNT_PROPERTY = "fieldCount";
  /**
   * Default number of fields in a record.
   */
  public static final String FIELD_COUNT_PROPERTY_DEFAULT = "10";
  /**
   * The name of the property for the field length distribution. Options are
   * "uniform", "zipfian" (favoring short records), "constant", and
   * "histogram".
   *
   * If "uniform", "zipfian" or "constant", the maximum field length will be
   * that specified by the fieldlength property. If "histogram", then the
   * histogram will be read from the filename specified in the
   * "fieldlengthhistogram" property.
   */
  public static final String FIELD_LENGTH_DISTRIBUTION_PROPERTY = "fieldLengthDistribution";
  /**
   * The default field length distribution.
   */
  public static final String FIELD_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT = "constant";
  /**
   * The name of the property for the length of a field in bytes.
   */
  public static final String FIELD_LENGTH_PROPERTY = "fieldLength";
  /**
   * The default maximum length of a field in bytes.
   */
  public static final String FIELD_LENGTH_PROPERTY_DEFAULT = "100";
  /**
   * The name of the property for the total amount of money in the economy at the start.
   */
  public static final String TOTAL_CASH_PROPERTY = "total_cash";
  /**
   * The default total amount of money in the economy at the start.
   */
  public static final String TOTAL_CASH_PROPERTY_DEFAULT = "1000000";
  public static final String OPERATION_COUNT_PROPERTY = "operationCount";
  /**
   * The name of a property that specifies the filename containing the field
   * length histogram (only used if fieldlengthdistribution is "histogram").
   */
  public static final String FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY = "fieldLengthHistogram";
  /**
   * The default filename containing a field length histogram.
   */
  public static final String FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY_DEFAULT = "hist.txt";
  /**
   * The name of the property for deciding whether to read one field (false)
   * or all fields (true) of a record.
   */
  public static final String READ_ALL_FIELDS_PROPERTY = "readAllFields";
  /**
   * The default value for the readAllFields property.
   */
  public static final String READ_ALL_FIELDS_PROPERTY_DEFAULT = "true";
  /**
   * The name of the property for deciding whether to write one field (false)
   * or all fields (true) of a record.
   */
  public static final String WRITE_ALL_FIELDS_PROPERTY = "writeAllFields";
  /**
   * The default value for the writeAllFields property.
   */
  public static final String WRITE_ALL_FIELDS_PROPERTY_DEFAULT = "false";
  /**
   * The name of the property for the proportion of transactions that are
   * reads.
   */
  public static final String READ_PROPORTION_PROPERTY = "readProportion";
  /**
   * The default proportion of transactions that are reads.
   */
  public static final String READ_PROPORTION_PROPERTY_DEFAULT = "0.95";
  /**
   * The name of the property for the proportion of transactions that are
   * updates.
   */
  public static final String UPDATE_PROPORTION_PROPERTY = "updateProportion";
  /**
   * The default proportion of transactions that are updates.
   */
  public static final String UPDATE_PROPORTION_PROPERTY_DEFAULT = "0.05";
  /**
   * The name of the property for the proportion of transactions that are
   * inserts.
   */
  public static final String INSERT_PROPORTION_PROPERTY = "insertProportion";
  /**
   * The default proportion of transactions that are inserts.
   */
  public static final String INSERT_PROPORTION_PROPERTY_DEFAULT = "0.0";
  /**
   * The name of the property for the proportion of transactions that are
   * scans.
   */
  public static final String SCAN_PROPORTION_PROPERTY = "scanProportion";
  /**
   * The default proportion of transactions that are scans.
   */
  public static final String SCAN_PROPORTION_PROPERTY_DEFAULT = "0.0";
  /**
   * The name of the property for the proportion of transactions that are
   * read-modify-write.
   */
  public static final String READMODIFYWRITE_PROPORTION_PROPERTY = "readModifyWriteProportion";
  /**
   * The default proportion of transactions that are scans.
   */
  public static final String READMODIFYWRITE_PROPORTION_PROPERTY_DEFAULT = "0.0";
  /**
   * The name of the property for the the distribution of requests across the
   * keyspace. Options are "uniform", "zipfian" and "latest"
   */
  public static final String REQUEST_DISTRIBUTION_PROPERTY = "requestdistribution";
  /**
   * The value of theta used if a zipfian distribution is used to generate the requests.
   */
  public static final String ZIPFIAN_REQUEST_DISTRIBUTION_THETA = "zipfianrequestdistributiontheta";
  /**
   * The default value of theta used if a zipfian distribution is used to generate the requests.
   */
  public static final String ZIPFIAN_REQUEST_DISTRIBUTION_THETA_DEFAULT = "0.99";
  /**
   * The default distribution of requests across the keyspace
   */
  public static final String REQUEST_DISTRIBUTION_PROPERTY_DEFAULT = "uniform";
  /**
   * The name of the property for the max scan length (number of records)
   */
  public static final String MAX_SCAN_LENGTH_PROPERTY = "maxscanlength";
  /**
   * The default max scan length.
   */
  public static final String MAX_SCAN_LENGTH_PROPERTY_DEFAULT = "1000";
  /**
   * The name of the property for the scan length distribution. Options are
   * "uniform" and "zipfian" (favoring short scans)
   */
  public static final String SCAN_LENGTH_DISTRIBUTION_PROPERTY = "scanlengthdistribution";
  /**
   * The default max scan length.
   */
  public static final String SCAN_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT = "uniform";
  /**
   * The name of the property for the order to insert records. Options are
   * "ordered" or "hashed"
   */
  public static final String INSERT_ORDER_PROPERTY = "insertorder";
  /**
   * Default insert order.
   */
  public static final String INSERT_ORDER_PROPERTY_DEFAULT = "hashed";
  /**
   * Percentage data items that constitute the hot set.
   */
  public static final String HOTSPOT_DATA_FRACTION = "hotspotdatafraction";
  /**
   * Default value of the size of the hot set.
   */
  public static final String HOTSPOT_DATA_FRACTION_DEFAULT = "0.2";
  /**
   * Percentage operations that access the hot set.
   */
  public static final String HOTSPOT_OPN_FRACTION = "hotspotopnfraction";
  /**
   * Default value of the percentage operations accessing the hot set.
   */
  public static final String HOTSPOT_OPN_FRACTION_DEFAULT = "0.8";
  public static String table;
  long fieldCount;
  /**
   * Generator object that produces field lengths. The value of this depends
   * on the properties that start with "FIELD_LENGTH_".
   */
  NumberGenerator fieldLengthGenerator;
  boolean readAllFields;
  boolean writeAllFields;
  NumberGenerator keysequence;
  NumberGenerator validation_keysequence;
  DiscreteGenerator operationchooser;
  NumberGenerator keychooser;
  Generator fieldchooser;
  CounterGenerator transactioninsertkeysequence;
  NumberGenerator scanlength;
  boolean orderedinserts;
  long recordcount;
  long opcount;
  AtomicInteger actualopcount = new AtomicInteger(0);
  private Measurements measurements;
  private Hashtable<String, String> operations = new Hashtable<String, String>() {
    {
      put("READ", "TX-READ");
      put("UPDATE", "TX-UPDATE");
      put("INSERT", "TX-INSERT");
      put("SCAN", "TX-SCAN");
      put("READMODIFYWRITE", "TX-READMODIFYWRITE");
    }
  };
  private long totalcash;
  private long currenttotal;
  private long currentcount;
  private long initialvalue;

  protected static NumberGenerator getFieldLengthGenerator(Properties p)
      throws WorkloadException {
    NumberGenerator fieldlengthgenerator;
    String fieldlengthdistribution = p.getProperty(FIELD_LENGTH_DISTRIBUTION_PROPERTY,
        FIELD_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT);

    int num_records = Integer.parseInt(p.getProperty(Client.RECORD_COUNT_PROPERTY));
    int total_cash = Integer.parseInt(
        p.getProperty(TOTAL_CASH_PROPERTY, TOTAL_CASH_PROPERTY_DEFAULT));

    long fieldlength = Long.parseLong(
        p.getProperty(FIELD_LENGTH_PROPERTY, FIELD_LENGTH_PROPERTY_DEFAULT));
    String fieldlengthhistogram = p.getProperty(FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY,
        FIELD_LENGTH_HISTOGRAM_FILE_PROPERTY_DEFAULT);
    if (fieldlengthdistribution.compareTo("constant") == 0) {
      fieldlengthgenerator = new ConstantIntegerGenerator(total_cash / num_records);
    } else if (fieldlengthdistribution.compareTo("uniform") == 0) {
      fieldlengthgenerator = new UniformLongGenerator(1, total_cash / num_records);
    } else if (fieldlengthdistribution.compareTo("zipfian") == 0) {
      fieldlengthgenerator = new ZipfianGenerator(1, fieldlength);
    } else if (fieldlengthdistribution.compareTo("histogram") == 0) {
      try {
        fieldlengthgenerator = new HistogramGenerator(fieldlengthhistogram);
      } catch (IOException e) {
        throw new WorkloadException(
            "Couldn't read field length histogram file: " + fieldlengthhistogram, e);
      }
    } else {
      throw new WorkloadException(
          "Unknown field length distribution \"" + fieldlengthdistribution + "\"");
    }
    return fieldlengthgenerator;
  }

  /**
   * Initialize the scenario. Called once, in the main client thread, before
   * any operations are started.
   */
  public void init(Properties p) throws WorkloadException {
    table = p.getProperty(TABLE_NAME_PROPERTY, TABLE_NAME_PROPERTY_DEFAULT);

    fieldCount = Long.parseLong(p.getProperty(FIELD_COUNT_PROPERTY, FIELD_COUNT_PROPERTY_DEFAULT));
    fieldLengthGenerator = ClosedEconomyWorkload.getFieldLengthGenerator(p);

    double readproportion = Double.parseDouble(
        p.getProperty(READ_PROPORTION_PROPERTY, READ_PROPORTION_PROPERTY_DEFAULT));
    double updateproportion = Double.parseDouble(
        p.getProperty(UPDATE_PROPORTION_PROPERTY, UPDATE_PROPORTION_PROPERTY_DEFAULT));
    double insertproportion = Double.parseDouble(
        p.getProperty(INSERT_PROPORTION_PROPERTY, INSERT_PROPORTION_PROPERTY_DEFAULT));
    double scanproportion = Double.parseDouble(
        p.getProperty(SCAN_PROPORTION_PROPERTY, SCAN_PROPORTION_PROPERTY_DEFAULT));
    double readmodifywriteproportion = Double.parseDouble(
        p.getProperty(READMODIFYWRITE_PROPORTION_PROPERTY,
            READMODIFYWRITE_PROPORTION_PROPERTY_DEFAULT));

    opcount = Long.parseLong(p.getProperty(OPERATION_COUNT_PROPERTY, "0"));
    recordcount = Long.parseLong(p.getProperty(Client.RECORD_COUNT_PROPERTY));
    totalcash = Long.parseLong(p.getProperty(TOTAL_CASH_PROPERTY, TOTAL_CASH_PROPERTY_DEFAULT));
    currenttotal = totalcash;
    currentcount = recordcount;
    initialvalue = totalcash / recordcount;

    String requestdistrib = p.getProperty(REQUEST_DISTRIBUTION_PROPERTY,
        REQUEST_DISTRIBUTION_PROPERTY_DEFAULT);
    long maxscanlength = Long.parseLong(
        p.getProperty(MAX_SCAN_LENGTH_PROPERTY, MAX_SCAN_LENGTH_PROPERTY_DEFAULT));
    String scanlengthdistrib = p.getProperty(SCAN_LENGTH_DISTRIBUTION_PROPERTY,
        SCAN_LENGTH_DISTRIBUTION_PROPERTY_DEFAULT);

    long insertstart = Long.parseLong(
        p.getProperty(INSERT_START_PROPERTY, INSERT_START_PROPERTY_DEFAULT));

    readAllFields = Boolean.parseBoolean(
        p.getProperty(READ_ALL_FIELDS_PROPERTY, READ_ALL_FIELDS_PROPERTY_DEFAULT));
    writeAllFields = Boolean.parseBoolean(
        p.getProperty(WRITE_ALL_FIELDS_PROPERTY, WRITE_ALL_FIELDS_PROPERTY_DEFAULT));

    if (p.getProperty(INSERT_ORDER_PROPERTY, INSERT_ORDER_PROPERTY_DEFAULT).compareTo("hashed")
        == 0) {
      orderedinserts = false;
    } else if (requestdistrib.compareTo("exponential") == 0) {
      double percentile = Double.parseDouble(
          p.getProperty(ExponentialGenerator.EXPONENTIAL_PERCENTILE_PROPERTY,
              ExponentialGenerator.EXPONENTIAL_PERCENTILE_DEFAULT));
      double frac = Double.parseDouble(p.getProperty(ExponentialGenerator.EXPONENTIAL_FRAC_PROPERTY,
          ExponentialGenerator.EXPONENTIAL_FRAC_DEFAULT));
      keychooser = new ExponentialGenerator(percentile, recordcount * frac);
    } else {
      orderedinserts = true;
    }

    keysequence = new CounterGenerator(insertstart);
    validation_keysequence = new CounterGenerator(insertstart);
    operationchooser = new DiscreteGenerator();
    if (readproportion > 0) {
      operationchooser.addValue(readproportion, "READ");
    }

    if (updateproportion > 0) {
      operationchooser.addValue(updateproportion, "UPDATE");
    }

    if (insertproportion > 0) {
      operationchooser.addValue(insertproportion, "INSERT");
    }

    if (readmodifywriteproportion > 0) {
      operationchooser.addValue(readmodifywriteproportion, "READMODIFYWRITE");
    }

    transactioninsertkeysequence = new CounterGenerator(recordcount);
    if (requestdistrib.compareTo("uniform") == 0) {
      keychooser = new UniformLongGenerator(0, recordcount - 1);
    } else if (requestdistrib.compareTo("zipfian") == 0) {
      // it does this by generating a random "next key" in part by taking the modulus over the number of keys
      // if the number of keys changes, this would shift the modulus, and we don't want that to change which keys are popular
      // so we'll actually construct the scrambled zipfian generator with a keyspace that is larger than exists at the beginning
      // of the test. that is, we'll predict the number of inserts, and tell the scrambled zipfian generator the number of existing keys
      // plus the number of predicted keys as the total keyspace. then, if the generator picks a key that hasn't been inserted yet, will
      // just ignore it and pick another key. this way, the size of the keyspace doesn't change from the perspective of the scrambled
      // zipfian generator
      long opcount = Long.parseLong(p.getProperty(Client.OPERATION_COUNT_PROPERTY));
      long expectednewkeys = (long) (((double) opcount) * insertproportion
          * 2.0); // 2 is fudge factor

      long theta = Long.parseLong(p.getProperty(ZIPFIAN_REQUEST_DISTRIBUTION_THETA,
          ZIPFIAN_REQUEST_DISTRIBUTION_THETA_DEFAULT));

      keychooser = new ScrambledZipfianGenerator(recordcount + expectednewkeys, theta);
    } else if (requestdistrib.compareTo("latest") == 0) {
      keychooser = new SkewedLatestGenerator(transactioninsertkeysequence);
    } else if (requestdistrib.equals("hotspot")) {
      double hotsetfraction = Double.parseDouble(
          p.getProperty(HOTSPOT_DATA_FRACTION, HOTSPOT_DATA_FRACTION_DEFAULT));
      double hotopnfraction = Double.parseDouble(
          p.getProperty(HOTSPOT_OPN_FRACTION, HOTSPOT_OPN_FRACTION_DEFAULT));
      keychooser = new HotspotIntegerGenerator(0, recordcount - 1, hotsetfraction, hotopnfraction);
    } else {
      throw new WorkloadException("Unknown request distribution \"" + requestdistrib + "\"");
    }

    fieldchooser = new UniformLongGenerator(0, fieldCount - 1);

    if (scanlengthdistrib.compareTo("uniform") == 0) {
      scanlength = new UniformLongGenerator(1, maxscanlength);
    } else if (scanlengthdistrib.compareTo("zipfian") == 0) {
      scanlength = new ZipfianGenerator(1, maxscanlength);
    } else {
      throw new WorkloadException(
          "Distribution \"" + scanlengthdistrib + "\" not allowed for scan length");
    }

    measurements = Measurements.getMeasurements();
  }

  public String buildKeyName(long keynum) {
//		if (!orderedinserts) {
//			keynum = Utils.hash(keynum);
//		}
    // System.err.println("key: " + keynum);
    return "user" + keynum;
  }

  HashMap<String, ByteIterator> buildValues() {
    HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();

    String fieldkey = "field0";
    ByteIterator data = new StringByteIterator("" + initialvalue);
    values.put(fieldkey, data);
    return values;
  }

  HashMap<String, ByteIterator> buildUpdate() {
    // update a random field
    HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
    String fieldname = "field" + fieldchooser.nextString();
    ByteIterator data = new RandomByteIterator(
        fieldLengthGenerator.nextValue().longValue());
    values.put(fieldname, data);
    return values;
  }

  /**
   * Do one insert operation. Because it will be called concurrently from
   * multiple client threads, this function must be thread safe. However,
   * avoid synchronized, or the threads will block waiting for each other, and
   * it will be difficult to reach the target throughput. Ideally, this
   * function would have no side effects other than DB operations.
   *
   */
  public boolean doInsert(DB db, Object threadstate) {
    long keynum = keysequence.nextValue().longValue();
    String dbkey = buildKeyName(keynum);
    HashMap<String, ByteIterator> values = buildValues();
    if (db.insert(table, dbkey, values).isOk()) {
      actualopcount.addAndGet(1);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Do one insert operation. Because it will be called concurrently from
   * multiple client threads, this function must be thread safe. However,
   * avoid synchronized, or the threads will block waiting for each other, and
   * it will be difficult to reach the target throughput. Ideally, this
   * function would have no side effects other than DB operations.
   *
   * @throws WorkloadException
   */
  public boolean doDelete(DB db, Object threadstate) throws WorkloadException {
    long keynum = keysequence.nextValue().longValue();
    String dbkey = buildKeyName(keynum);
    if (db.delete(table, dbkey).isOk()) {
      actualopcount.addAndGet(1);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Do one transaction operation. Because it will be called concurrently from
   * multiple client threads, this function must be thread safe. However,
   * avoid synchronized, or the threads will block waiting for each other, and
   * it will be difficult to reach the target throughput. Ideally, this
   * function would have no side effects other than DB operations.
   *
   */
  public boolean doTransaction(DB db, Object threadstate) {
    boolean ret = true;
    long st = System.nanoTime();

    String op = operationchooser.nextString();

    if (op.compareTo("READ") == 0) {
      ret = doTransactionRead(db);
    } else if (op.compareTo("UPDATE") == 0) {
      ret = doTransactionUpdate(db);
    } else if (op.compareTo("INSERT") == 0) {
      ret = doTransactionInsert(db);
    } else if (op.compareTo("SCAN") == 0) {
      ret = doTransactionScan(db);
    } else {
      ret = doTransactionReadModifyWrite(db);
    }

    long en = System.nanoTime();
    measurements.measure(operations.get(op), (int) ((en - st) / 1000));
    // todo: not sure, -1 and 0 meaning in the new status
    if (ret) {
      measurements.reportStatus(operations.get(op), Status.ERROR);
    } else {
      measurements.reportStatus(operations.get(op), Status.OK);
    }
    actualopcount.addAndGet(1);

    return ret;
  }

  long nextKeyNum() {
    long keynum;
    if (keychooser instanceof ExponentialGenerator) {
      do {
        keynum = transactioninsertkeysequence.lastValue().longValue()
            - keychooser.nextValue().longValue();
      } while (keynum < 0);
    } else {
      do {
        keynum = keychooser.nextValue().longValue();
      } while (keynum > transactioninsertkeysequence.lastValue().longValue());
    }
    return keynum;
  }

  public boolean doTransactionRead(DB db) {
    // choose a random key
    long keynum = nextKeyNum();

    String keyname = buildKeyName(keynum);

    HashSet<String> fields = null;

    if (!readAllFields) {
      // read a random field
      String fieldname = "field" + fieldchooser.nextString();

      fields = new HashSet<String>();
      fields.add(fieldname);
    }

    HashMap<String, ByteIterator> firstvalues = new HashMap<String, ByteIterator>();

    return (db.read(table, keyname, fields, firstvalues).isOk());
  }

  public boolean doTransactionReadModifyWrite(DB db) {
    // choose a random key
    long first = nextKeyNum();
    long second = first;
    while (second == first) {
      second = nextKeyNum();
    }
    if (first < second) {
      long temp = first;
      first = second;
      second = temp;
    }

    String firstkey = buildKeyName(first);
    String secondkey = buildKeyName(second);

    HashSet<String> fields = null;

    if (!readAllFields) {
      // read a random field
      String fieldname = "field" + fieldchooser.nextString();

      fields = new HashSet<String>();
      fields.add(fieldname);
    }

    HashMap<String, ByteIterator> firstvalues = new HashMap<String, ByteIterator>();
    HashMap<String, ByteIterator> secondvalues = new HashMap<String, ByteIterator>();

    // do the transaction
    long st = System.currentTimeMillis();

    if (db.read(table, firstkey, fields, firstvalues).isOk() && db.read(table, secondkey, fields,
        secondvalues).isOk()) {
      try {
        long firstamount = Long.parseLong(firstvalues.get("field0")
            .toString());
        long secondamount = Long.parseLong(secondvalues.get("field0")
            .toString());

        if (firstamount > 0) {
          firstamount--;
          secondamount++;
        }

        firstvalues.put("field0",
            new StringByteIterator(Long.toString(firstamount)));
        secondvalues.put("field0",
            new StringByteIterator(Long.toString(secondamount)));

        if (db.update(table, firstkey, firstvalues).isOk() ||
            db.update(table, secondkey, secondvalues).isOk()) {
          return false;
        }

        long en = System.currentTimeMillis();

        Measurements.getMeasurements().measure("READ-MODIFY-WRITE",
            (int) (en - st));
      } catch (NumberFormatException e) {
        return false;
      }
      return true;
    }
    return false;
  }

  public boolean doTransactionScan(DB db) {
    // choose a random key
    long keynum = nextKeyNum();

    String startkeyname = buildKeyName(keynum);

    // choose a random scan length
    int len = scanlength.nextValue().intValue();

    HashSet<String> fields = null;

    if (!readAllFields) {
      // read a random field
      String fieldname = "field" + fieldchooser.nextString();

      fields = new HashSet<String>();
      fields.add(fieldname);
    }

    return (db.scan(table, startkeyname, len, fields,
        new Vector<HashMap<String, ByteIterator>>()).isOk());
  }

  public boolean doTransactionUpdate(DB db) {
    // choose a random key
    long keynum = nextKeyNum();

    String keyname = buildKeyName(keynum);

    HashMap<String, ByteIterator> values;

    if (writeAllFields) {
      // new data for all the fields
      values = buildValues();
    } else {
      // update a random field
      values = buildUpdate();
    }

    return (db.update(table, keyname, values).isOk());
  }

  public boolean doTransactionInsert(DB db) {
    // choose the next key
    long keynum = transactioninsertkeysequence.nextValue();

    String dbkey = buildKeyName(keynum);

    HashMap<String, ByteIterator> values = buildValues();
    return (db.insert(table, dbkey, values).isOk());
  }

  /**
   * Perform validation of the database db after the workload has executed.
   *
   * @return false if the workload left the database in an inconsistent state, true if it is consistent.
   * @throws WorkloadException
   */
  public boolean validate(DB db) throws WorkloadException {
    HashSet<String> fields = new HashSet<String>();
    fields.add("field0");
    System.out.println("Validating data");
    HashMap<String, ByteIterator> values = new HashMap<String, ByteIterator>();
    long counted_sum = 0;
    for (long i = 0; i < recordcount; i++) {
      String keyname = buildKeyName(validation_keysequence.nextValue().longValue());
      try {
        db.start();
        db.read(table, keyname, fields, values);
        db.commit();
      } catch (DBException e) {
        throw new WorkloadException(e);
      }
      counted_sum += Long.parseLong(values.get("field0").toString());
    }

    if (counted_sum != totalcash) {
      System.out.println("Validation failed");
      System.out.println("[TOTAL CASH], " + totalcash);
      System.out.println("[COUNTED CASH], " + counted_sum);
      long count = actualopcount.intValue();
      System.out.println("[ACTUAL OPERATIONS], " + count);
      System.out.println("[ANOMALY SCORE], " + Math.abs((totalcash - counted_sum) / (1.0 * count)));
      return false;
    } else {
      return true;
    }
  }
}
