// Copyright (C) 2000 Phil Dawes
// Copyright (C) 2000, 2001, 2002 Philip Aston
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

package net.grinder.tools.tcpsniffer;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import net.grinder.util.TerminalColour;


/**
 *
 * @author Phil Dawes
 * @author Philip Aston
 * @version $Revision$
 */
public class SnifferEngineImplementation implements SnifferEngine
{
    private final SnifferFilter m_requestFilter;
    private final SnifferFilter m_responseFilter;
    private final ConnectionDetails m_connectionDetails;
    private final boolean m_useColour;

    private final PrintWriter m_outputWriter;

    private final SnifferSocketFactory m_socketFactory;
    private final ServerSocket m_serverSocket;

    public SnifferEngineImplementation(SnifferSocketFactory socketFactory,
				       SnifferFilter requestFilter,
				       SnifferFilter responseFilter,
				       ConnectionDetails connectionDetails,
				       boolean useColour,
				       int timeout)
	throws IOException
    {
	m_socketFactory = socketFactory;
	m_requestFilter = requestFilter;
	m_responseFilter = responseFilter;
	m_connectionDetails = connectionDetails;
	m_useColour = useColour;

	m_outputWriter = new PrintWriter(System.out);
	requestFilter.setOutputPrintWriter(m_outputWriter);
	responseFilter.setOutputPrintWriter(m_outputWriter);

	m_serverSocket =
	    m_socketFactory.createServerSocket(
		connectionDetails.getLocalHost(),
		connectionDetails.getLocalPort(),
		timeout);
    }
    
    public void run()
    {
	while (true) {
	    final Socket localSocket;

	    try {
		localSocket = m_serverSocket.accept();
	    }
	    catch (IOException e) {
		e.printStackTrace(System.err);
		return;
	    }

	    try {
		launchThreadPair(localSocket,
				 localSocket.getInputStream(),
				 localSocket.getOutputStream(),
				 m_connectionDetails.getRemoteHost(),
				 m_connectionDetails.getRemotePort());
	    }
	    catch(IOException e) {
		e.printStackTrace(System.err);
	    }
	}
    }

    public final ServerSocket getServerSocket() 
    {
	return m_serverSocket;
    }

    protected final void launchThreadPair(Socket localSocket,
					  InputStream localInputStream,
					  OutputStream localOutputStream,
					  String remoteHost,
					  int remotePort)
	throws IOException
    {
	final Socket remoteSocket =
	    m_socketFactory.createClientSocket(remoteHost, remotePort);

	new StreamThread(new ConnectionDetails(
			     m_connectionDetails.getLocalHost(),
			     localSocket.getPort(),
			     remoteHost,
			     remoteSocket.getPort(),
			     m_connectionDetails.isSecure()),
			 localInputStream,
			 remoteSocket.getOutputStream(),
			 m_requestFilter,
			 m_outputWriter,
			 getColourString(false));

	new StreamThread(new ConnectionDetails(
			     remoteHost,
			     remoteSocket.getPort(),
			     m_connectionDetails.getLocalHost(),
			     localSocket.getPort(),
			     m_connectionDetails.isSecure()),
			 remoteSocket.getInputStream(),
			 localOutputStream,
			 m_responseFilter,
			 m_outputWriter,
			 getColourString(true));
    }

    private String getColourString(boolean response)
    {
	if (!m_useColour) {
	    return "";
	}
	else {
	    return response ? TerminalColour.BLUE : TerminalColour.RED;
	}
    }
}

