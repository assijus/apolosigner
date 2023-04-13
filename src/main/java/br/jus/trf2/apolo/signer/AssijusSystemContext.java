package br.jus.trf2.apolo.signer;

import java.io.IOException;

import com.crivano.swaggerservlet.SwaggerContext;

public class AssijusSystemContext implements br.jus.trf2.assijus.system.api.AssijusSystemContext {

	@Override
	public void init(SwaggerContext ctx) {
	}

	@Override
	public void onTryBegin() throws Exception {
	}

	@Override
	public void onTryEnd() throws Exception {
	}

	@Override
	public void onCatch(Exception e) throws Exception {
		throw e;
	}

	@Override
	public void onFinally() throws Exception {
	}

	@Override
	public void close() throws IOException {
	}
 
}
