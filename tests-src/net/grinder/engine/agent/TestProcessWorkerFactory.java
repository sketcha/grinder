// Copyright (C) 2004, 2005 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.engine.agent;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import net.grinder.common.GrinderProperties;
import net.grinder.common.WorkerIdentity;
import net.grinder.communication.FanOutStreamSender;
import net.grinder.engine.common.EngineException;
import net.grinder.engine.messages.InitialiseGrinderMessage;
import net.grinder.engine.process.GrinderProcess;


/**
 *  Unit tests for <code>ProcessWorkerFactory</code>.
 *
 * @author Philip Aston
 * @version $Revision$
 */
public class TestProcessWorkerFactory extends TestCase {

  private static final String s_classesDir = System.getProperty("classes.dir");
  private static final String s_testClasspath =
    System.getProperty("java.class.path");

  public void testCreate() throws Exception {
    final GrinderProperties grinderProperties = new GrinderProperties();
    grinderProperties.setProperty(
      "grinder.jvm.classpath",
      s_testClasspath + File.pathSeparatorChar + s_classesDir);

    final Properties systemProperties = new Properties();

    final FanOutStreamSender fanOutStreamSender = new FanOutStreamSender(1);

    final WorkerProcessCommandLine commandLine =
      new WorkerProcessCommandLine(
        grinderProperties, systemProperties, "");

    final List commandList = commandLine.getCommandList();

    commandList.set(commandList.indexOf(GrinderProcess.class.getName()),
                    ReadMessageEchoClass.class.getName());

    final File scriptFile = new File("a");
    final File scriptDirectory = new File("b");
    final boolean reportToConsole = false;

    final AgentIdentityImplementation agentIdentityImplementation =
      new AgentIdentityImplementation(getClass().getName());

    final ProcessWorkerFactory processWorkerFactory =
      new ProcessWorkerFactory(commandLine,
                               agentIdentityImplementation,
                               fanOutStreamSender,
                               reportToConsole,
                               scriptFile,
                               scriptDirectory,
                               grinderProperties);

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

    final Worker worker =
      processWorkerFactory.create(outputStream, errorStream);

    assertTrue(worker.getIdentity().getName().endsWith("-0"));

    worker.waitFor();

    assertEquals("", new String(errorStream.toByteArray()));

    final InputStream output =
      new ByteArrayInputStream(outputStream.toByteArray());
    final ObjectInputStream objectInputStream = new ObjectInputStream(output);

    final InitialiseGrinderMessage echoedInitialiseGrinderMessage =
      (InitialiseGrinderMessage)objectInputStream.readObject();

    assertEquals(reportToConsole,
                 echoedInitialiseGrinderMessage.getReportToConsole());
    assertEquals(scriptDirectory,
                 echoedInitialiseGrinderMessage.getScriptDirectory());
    assertEquals(scriptFile,
                 echoedInitialiseGrinderMessage.getScriptFile());
    assertEquals(
      agentIdentityImplementation,
      echoedInitialiseGrinderMessage.getWorkerIdentity().getAgentIdentity());

    final byte[] remainingBytes = new byte[500];
    final int n = output.read(remainingBytes);

    assertEquals(-1, n); // No arguments.
    
    fanOutStreamSender.shutdown();
  }

  public void testBadWorker() throws Exception {
    // Test a dusty code path through AbstractWorkerFactory where
    // the Worker communication stream doesn't work.
    final AgentIdentityImplementation agentIdentityImplementation =
      new AgentIdentityImplementation(getClass().getName());

    final AbstractWorkerFactory myWorkerFactory =
      new AbstractWorkerFactory(agentIdentityImplementation,
                                null, false, null, null, null) {

        protected Worker createWorker(WorkerIdentity workerIdentity,
                                      OutputStream outputStream,
                                      OutputStream errorStream)
        throws EngineException {
          return new Worker() {

            public WorkerIdentity getIdentity() {
              return null;
            }

            public OutputStream getCommunicationStream() {
              return new OutputStream() {
                public void write(int b) throws IOException {
                  throw new IOException("Broken");
                }
              };
            }

            public int waitFor() throws InterruptedException, EngineException {
              return 0;
            }

            public void destroy() {
            }
          };
        }
    };

    try {
      myWorkerFactory.create(null, null);
      fail("Expected EngineException");
    }
    catch (EngineException e) {
    }
  }
}
