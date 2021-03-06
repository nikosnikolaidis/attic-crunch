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
package org.apache.crunch.io.text.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.crunch.MapFn;
import org.apache.crunch.impl.mr.MRPipeline;
import org.apache.crunch.impl.mr.run.RuntimeParameters;
import org.apache.crunch.io.To;
import org.apache.crunch.test.TemporaryPath;
import org.apache.crunch.types.writable.Writables;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.Lists;

@SuppressWarnings("serial")
public class XmlSourceIT implements Serializable {

  @Rule
  public transient TemporaryPath tmpDir = new TemporaryPath(RuntimeParameters.TMP_DIR, "hadoop.tmp.dir");

  private static final ArrayList<String> RESULT_ELEMENTS = new ArrayList<String>();

  private static final class TestMapFn extends MapFn<String, String> {
    @Override
    public String map(String input) {
      RESULT_ELEMENTS.add(input);
      return input;
    }
  }

  @Before
  public void beforeEachTest() {
    RESULT_ELEMENTS.clear();
  }

  @Test
  public void testPlainXmlUtf8() throws Exception {

    String xmlInFile = tmpDir.copyResourceFileName("xmlSourceSample1.xml");
    String outFile = tmpDir.getFileName("out");

    XmlSource xmlSource = new XmlSource(xmlInFile, "<PLANT", "</PLANT>");

    MRPipeline p = new MRPipeline(XmlSourceIT.class, tmpDir.getDefaultConfiguration());
    p.read(xmlSource).by(new TestMapFn(), Writables.strings()).write(To.textFile(outFile));
    p.done();

    assertEquals("[36] elements expected but found: " + RESULT_ELEMENTS.size(), 36, RESULT_ELEMENTS.size());
  }

  @Test
  public void testEncoding() throws Exception {

    final ArrayList<String> expectedElements = Lists.newArrayList("<??????????>One</??????????>        ",
        "<??????????>??????</??????????>     ", "<??????????>??????</??????????>     ");

    String xmlInFile = tmpDir.copyResourceFileName("xmlSourceSample2.xml");
    String outFile = tmpDir.getFileName("out");

    XmlSource xmlSource = new XmlSource(xmlInFile, "<??????????", "</??????????>", "UTF-16BE");

    MRPipeline p = new MRPipeline(XmlSourceIT.class, tmpDir.getDefaultConfiguration());
    p.read(xmlSource).by(new TestMapFn(), Writables.strings()).write(To.textFile(outFile));
    p.done();

    assertEquals(3, RESULT_ELEMENTS.size());
    assertTrue("Expected elements: " + expectedElements + " not found!", RESULT_ELEMENTS.containsAll(expectedElements));
  }
}
