package br.jus.trf2.apolo.signer;

import java.io.ByteArrayInputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;

import br.jus.trf2.apolo.signer.IApoloSigner.DocIdSignPutRequest;
import br.jus.trf2.apolo.signer.IApoloSigner.DocIdSignPutResponse;
import br.jus.trf2.apolo.signer.IApoloSigner.IDocIdSignPut;
import br.jus.trf2.apolo.signer.IApoloSigner.Warning;

import com.crivano.swaggerservlet.SwaggerUtils;

public class DocIdSignPut implements IDocIdSignPut {

	@Override
	public void run(DocIdSignPutRequest req, DocIdSignPutResponse resp)
			throws Exception {
		Id id = new Id(req.id);
		Extra extra = new Extra(req.extra);

		String envelope = SwaggerUtils.base64Encode(req.envelope);
		Date time = req.time;
		String name = req.name;
		String cpf = req.cpf;
		String sha1 = SwaggerUtils.base64Encode(req.sha1);

		byte[] assinatura = envelope.getBytes("UTF-8");

		byte[] envelopeCompressed = Utils.compress(assinatura);

		// O pdf precisará ser recuperado do cache apenas se ele não estiver
		// disponível na tabela do compressor automatico
		byte[] pdfCompressed = Utils.retrieve(sha1);
		if (pdfCompressed == null && extra.dthrultatu == null)
			throw new Exception("Não foi possível recuperar o PDF comprimido.");

		// Chama a procedure que faz a gravação da assinatura
		//
		Connection conn = null;
		CallableStatement cstmt = null;
		try {
			conn = Utils.getConnection();

			cstmt = conn.prepareCall(Utils.getSQL("save"));

			// p_CodSecao -> Código da Seção Judiciária (50=ES; 51=RJ;
			// 2=TRF)
			cstmt.setInt(1, id.codsecao);

			// p_CodDoc -> Código interno do documento
			cstmt.setLong(2, id.coddoc);

			// p_DtHrMov -> Data-Hora do movimento que se está assinando
			cstmt.setTimestamp(3, id.dthrmov);

			// p_Arq -> Arquivo PDF (Compactado)
			cstmt.setBlob(4, pdfCompressed == null ? null
					: new ByteArrayInputStream(pdfCompressed));

			// p_ArqAssin -> Arquivo de assinatura (Compactado)
			cstmt.setBlob(5, new ByteArrayInputStream(envelopeCompressed));

			// p_NumPagTotal -> Pode passar "null"
			cstmt.setInt(6, extra.pagecount);

			// p_NomeAssin -> Nome de quem assinou (obtido do certificado)
			cstmt.setString(7, name);

			// CPF
			cstmt.setString(8, cpf);

			// Data-Hora em que ocorreu a assinatura
			cstmt.setTimestamp(9, new Timestamp(time.getTime()));

			// Data-Hora da última atualização do arquivo do word, para impedir
			// que seja grava a assinatura de um documento que já sofreu
			// atualização
			cstmt.setTimestamp(10, extra.dthrultatu == null ? null
					: new Timestamp(extra.dthrultatu.getTime()));

			// Status
			cstmt.registerOutParameter(11, Types.VARCHAR);

			// Error
			cstmt.registerOutParameter(12, Types.VARCHAR);

			cstmt.execute();

			// Produce response
			resp.status = cstmt.getString(11);
			String errormsg = cstmt.getString(12);
			if (errormsg != null)
				throw new Exception(errormsg);
			if (extra.dthrultatu == null) {
				Warning warning = new Warning();
				warning.label = "pdf";
				warning.description = "Foi necessário criar o PDF dinamicamente.";
				resp.warning = new ArrayList<Warning>();
				resp.warning.add(warning);
			}
		} finally {
			if (cstmt != null)
				cstmt.close();
			if (conn != null)
				conn.close();
		}
	}

	@Override
	public String getContext() {
		return "salvar assinatura";
	}
}