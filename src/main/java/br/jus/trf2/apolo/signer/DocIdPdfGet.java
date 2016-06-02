package br.jus.trf2.apolo.signer;

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;
import com.crivano.restservlet.RestUtils;

public class DocIdPdfGet implements IRestAction {
	@Override
	public void run(HttpServletRequest request, HttpServletResponse response,
			JSONObject req, JSONObject resp) throws Exception {
		String status = null;
		String error = null;
		final boolean fForcePKCS7 = true;

		Id id = new Id(req.getString("id"));

		byte[] pdf = null;

		// Chama a procedure que recupera os dados do PDF
		//
		Connection conn = null;
		CallableStatement cstmt = null;
		Exception exception = null;
		try {
			conn = Utils.getConnection();

			cstmt = conn.prepareCall(Utils.getSQL("pdfinfo"));

			// p_CodSecao -> Código da Seção Judiciária (50=ES; 51=RJ;
			// 2=TRF)
			cstmt.setInt(1, id.codsecao);

			// p_CodDoc -> Código interno do documento
			cstmt.setLong(2, id.coddoc);

			// p_DtHrMov -> Data-Hora do movimento que se está assinando
			cstmt.setTimestamp(3, id.dthrmov);

			// CPF
			cstmt.setString(4, id.cpf);

			// Recuperar o PDF completo para permitir a assinatura sem política?
			cstmt.setInt(5, fForcePKCS7 ? 1 : 0);

			// SHA1
			cstmt.registerOutParameter(6, Types.VARCHAR);

			// SHA256
			cstmt.registerOutParameter(7, Types.VARCHAR);

			// Número de páginas
			cstmt.registerOutParameter(8, Types.NUMERIC);

			// Data hora da última atualização
			cstmt.registerOutParameter(9, Types.TIMESTAMP);

			// PDF uncompressed
			cstmt.registerOutParameter(10, Types.BLOB);

			// Status
			cstmt.registerOutParameter(11, Types.VARCHAR);

			// Error
			cstmt.registerOutParameter(12, Types.VARCHAR);

			cstmt.execute();

			// recupera o pdf para fazer assinatura sem política, apenas se ele
			// for diferente de null
			Blob blob = cstmt.getBlob(10);
			if (blob != null)
				pdf = blob.getBytes(1, (int) blob.length());
			status = cstmt.getString(11);
			error = cstmt.getString(12);
		} catch (Exception ex) {
			exception = ex;
			pdf = null;
		} finally {
			if (cstmt != null)
				cstmt.close();
			if (conn != null)
				conn.close();
		}

		if (pdf == null
				&& RestUtils.getProperty("apolosigner.pdfservice.url", null) != null) {
			byte[] docCompressed = null;

			// Get documents from Oracle
			conn = null;
			PreparedStatement pstmt = null;
			ResultSet rset = null;
			try {
				conn = Utils.getConnection();
				pstmt = conn.prepareStatement(Utils.getSQL("doc"));
				pstmt.setLong(1, id.coddoc);
				pstmt.setTimestamp(2, id.dthrmov);
				rset = pstmt.executeQuery();

				if (rset.next()) {
					Blob blob = rset.getBlob("TXTWORD");
					docCompressed = blob.getBytes(1L, (int) blob.length());
				} else {
					throw new Exception("Nenhum DOC encontrado.");
				}

				if (rset.next())
					throw new Exception("Mais de um DOC encontrado.");
			} finally {
				if (rset != null)
					rset.close();
				if (pstmt != null)
					pstmt.close();
				if (conn != null)
					conn.close();
			}

			if (docCompressed == null)
				throw new Exception("Não foi possível localizar o DOC.");

			// Decompress
			byte[] doc = Utils.decompress(docCompressed);

			if (doc == null)
				throw new Exception("Não foi possível descomprimir o DOC.");

			// Convert
			pdf = Utils.convertDocToPdf(doc);
			// pdf = Utils.convertDocToPdfUnoConv(doc);

			if (pdf == null)
				throw new Exception("Não foi possível converter para PDF.");
		}
		
		if (pdf == null && exception != null)
			throw exception;

		// Produce responses
		resp.put("doc", Base64.encodeBase64String(pdf));
	}

	@Override
	public String getContext() {
		return "visualizar documento do Apolo";
	}
}
