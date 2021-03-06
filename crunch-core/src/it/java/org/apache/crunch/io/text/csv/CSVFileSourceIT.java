/**
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
package org.apache.crunch.io.text.csv;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.crunch.PCollection;
import org.apache.crunch.PTable;
import org.apache.crunch.Pipeline;
import org.apache.crunch.impl.mr.MRPipeline;
import org.apache.crunch.test.TemporaryPath;
import org.apache.crunch.test.TemporaryPaths;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.io.compress.DeflateCodec;
import org.junit.Rule;
import org.junit.Test;

public class CSVFileSourceIT {
  @Rule
  public TemporaryPath tmpDir = TemporaryPaths.create();

  @Test
  public void testVanillaCSV() throws Exception {
    final String[] expectedFileContents = { "1,2,3,4", "5,6,7,8", "9,10,11", "12,13,14" };

    final String vanillaCSVFile = tmpDir.copyResourceFileName("vanilla.csv");
    final Pipeline pipeline = new MRPipeline(CSVFileSourceIT.class, tmpDir.getDefaultConfiguration());
    final PCollection<String> csvLines = pipeline.read(new CSVFileSource(new Path(vanillaCSVFile)));

    final Collection<String> csvLinesList = csvLines.asCollection().getValue();

    for (int i = 0; i < expectedFileContents.length; i++) {
      assertTrue(csvLinesList.contains(expectedFileContents[i]));
    }
  }

  @Test
  public void testVanillaCSV_Compressed() throws Exception {
    final String[] expectedFileContents = { "1,2,3,4", "5,6,7,8", "9,10,11", "12,13,14" };

    CompressionCodecFactory codecFactory = new CompressionCodecFactory(tmpDir.getDefaultConfiguration());
    CompressionCodec deflateCodec = codecFactory.getCodecByName(DeflateCodec.class.getName());

    File compressedFile = tmpDir.getFile("vanilla." + deflateCodec.getDefaultExtension());

    InputStream in = CSVFileSourceIT.class.getClassLoader().getResourceAsStream("vanilla.csv");
    OutputStream out = deflateCodec.createOutputStream(new FileOutputStream(compressedFile));
    try {
      IOUtils.copy(in, out);
    }finally {
      in.close();
      out.close();
    }

    final String vanillaCSVFile = tmpDir.copyResourceFileName("vanilla.csv");
    final Pipeline pipeline = new MRPipeline(CSVFileSourceIT.class, tmpDir.getDefaultConfiguration());
    final PCollection<String> csvLines = pipeline.read(new CSVFileSource(new Path(compressedFile.getPath())));

    final Collection<String> csvLinesList = csvLines.asCollection().getValue();

    for (int i = 0; i < expectedFileContents.length; i++) {
      assertTrue(csvLinesList.contains(expectedFileContents[i]));
    }
  }

  @Test
  public void testVanillaCSVWithAdditionalActions() throws Exception {
    final String[] expectedFileContents = { "1,2,3,4", "5,6,7,8", "9,10,11", "12,13,14" };

    final String vanillaCSVFile = tmpDir.copyResourceFileName("vanilla.csv");
    final Pipeline pipeline = new MRPipeline(CSVFileSourceIT.class, tmpDir.getDefaultConfiguration());
    final PCollection<String> csvLines = pipeline.read(new CSVFileSource(new Path(vanillaCSVFile)));

    final PTable<String, Long> countTable = csvLines.count();
    final PCollection<String> csvLines2 = countTable.keys();
    final Collection<String> csvLinesList = csvLines2.asCollection().getValue();

    for (int i = 0; i < expectedFileContents.length; i++) {
      assertTrue(csvLinesList.contains(expectedFileContents[i]));
    }
  }

  @Test
  public void testCSVWithNewlines() throws Exception {
    final String[] expectedFileContents = {
        "\"Champion, Mac\",\"1234 Hoth St.\n\tApartment 101\n\tAtlanta, GA\n\t64086\",\"30\",\"M\",\"5/28/2010 12:00:00 AM\",\"Just some guy\"",
        "\"Champion, Mac\",\"5678 Tatooine Rd. Apt 5, Mobile, AL 36608\",\"30\",\"M\",\"Some other date\",\"short description\"" };

    final String csvWithNewlines = tmpDir.copyResourceFileName("withNewlines.csv");
    final Pipeline pipeline = new MRPipeline(CSVFileSourceIT.class, tmpDir.getDefaultConfiguration());
    final PCollection<String> csvLines = pipeline.read(new CSVFileSource(new Path(csvWithNewlines)));

    final Collection<String> csvLinesList = csvLines.asCollection().getValue();

    for (int i = 0; i < expectedFileContents.length; i++) {
      assertTrue(csvLinesList.contains(expectedFileContents[i]));
    }
  }

  /**
   * This test is to make sure that custom char values set in the FileSource are
   * successfully picked up and used later by the InputFormat.
   */
  @Test
  public void testCSVWithCustomQuoteAndNewlines() throws IOException {
    final String[] expectedFileContents = {
        "*Champion, Mac*,*1234 Hoth St.\n\tApartment 101\n\tAtlanta, GA\n\t64086*,*30*,*M*,*5/28/2010 12:00:00 AM*,*Just some guy*",
        "*Mac, Champion*,*5678 Tatooine Rd. Apt 5, Mobile, AL 36608*,*30*,*M*,*Some other date*,*short description*" };

    final String csvWithNewlines = tmpDir.copyResourceFileName("customQuoteCharWithNewlines.csv");
    final Pipeline pipeline = new MRPipeline(CSVFileSourceIT.class, tmpDir.getDefaultConfiguration());
    final PCollection<String> csvLines = pipeline.read(new CSVFileSource(new Path(csvWithNewlines),
        CSVLineReader.DEFAULT_BUFFER_SIZE, CSVLineReader.DEFAULT_INPUT_FILE_ENCODING, '*', '*',
        CSVLineReader.DEFAULT_ESCAPE_CHARACTER, CSVLineReader.DEFAULT_MAXIMUM_RECORD_SIZE));

    final Collection<String> csvLinesList = csvLines.asCollection().getValue();

    for (int i = 0; i < expectedFileContents.length; i++) {
      assertTrue(csvLinesList.contains(expectedFileContents[i]));
    }
  }

  /**
   * This is effectively a mirror the above address tests, but using Chinese
   * characters, even for the quotation marks and escape characters.
   * 
   * @throws IOException
   */
  @Test
  public void testBrokenLineParsingInChinese() throws IOException {
    final String[] expectedChineseLines = { "???????????????????????????????????????????????????????????????????????????????????????", "??????????????????????????????????????????????????????????????????",
        "?????????????????????????????????????????????\n?????????\n?????????\n????????????\n???????????????????????????????????????????????????????????????????????????", "?????????????????????????????????????????????????????????????????????????????????????????????" };
    final String chineseLines = tmpDir.copyResourceFileName("brokenChineseLines.csv");

    final Pipeline pipeline = new MRPipeline(CSVFileSourceIT.class, tmpDir.getDefaultConfiguration());
    final PCollection<String> csvLines = pipeline.read(new CSVFileSource(new Path(chineseLines),
        CSVLineReader.DEFAULT_BUFFER_SIZE, CSVLineReader.DEFAULT_INPUT_FILE_ENCODING, '???', '???', '???',
        CSVLineReader.DEFAULT_MAXIMUM_RECORD_SIZE));
    final Collection<String> csvLinesList = csvLines.asCollection().getValue();
    for (int i = 0; i < expectedChineseLines.length; i++) {
      assertTrue(csvLinesList.contains(expectedChineseLines[i]));
    }
  }
}