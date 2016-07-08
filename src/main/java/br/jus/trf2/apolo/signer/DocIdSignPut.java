package br.jus.trf2.apolo.signer;

import java.io.ByteArrayInputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;

public class DocIdSignPut implements IRestAction {

	@Override
	public void run(JSONObject req, JSONObject resp) throws Exception {
		Id id = new Id(req.getString("id"));

		String envelope = req.getString("envelope");
		String time = req.getString("time");
		String name = req.getString("name");
		String cpf = req.getString("cpf");
		String sha1 = req.getString("sha1");

		Date dtSign = javax.xml.bind.DatatypeConverter.parseDateTime(time)
				.getTime();

		byte[] assinatura = envelope.getBytes("UTF-8");

		byte[] envelopeCompressed = Utils.compress(assinatura);

		// O pdf precisará ser recuperado do cache apenas se ele não estiver
		// disponível na tabela do compressor automatico
		byte[] pdfCompressed = Utils.retrieve(sha1);
		if (pdfCompressed == null && id.dthrultatu == null)
			throw new Exception("Não foi possível recuperar o PDF comprimido.");

		String msg = null;

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
			cstmt.setInt(6, id.pagecount);

			// p_NomeAssin -> Nome de quem assinou (obtido do certificado)
			cstmt.setString(7, name);

			// CPF
			cstmt.setString(8, cpf);

			// Data-Hora em que ocorreu a assinatura
			cstmt.setTimestamp(9, new Timestamp(dtSign.getTime()));

			// Data-Hora da última atualização do arquivo do word, para impedir
			// que seja grava a assinatura de um documento que já sofreu
			// atualização
			cstmt.setTimestamp(10, id.dthrultatu == null ? null
					: new Timestamp(id.dthrultatu.getTime()));

			// Status
			cstmt.registerOutParameter(11, Types.VARCHAR);

			// Error
			cstmt.registerOutParameter(12, Types.VARCHAR);

			cstmt.execute();

			// Produce response
			resp.put("status", cstmt.getObject(11));
			resp.put("errormsg", cstmt.getObject(12));
			if (id.dthrultatu == null) {
				JSONArray arr = new JSONArray();
				JSONObject obj = new JSONObject();
				obj.put("label", "pdf");
				obj.put("description",
						"Foi necessário criar o PDF dinamicamente.");
				arr.put(obj);
				resp.put("warning", arr);
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
		return "salvar assinatura no apolo";
	}
}