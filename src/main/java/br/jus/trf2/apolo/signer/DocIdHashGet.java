package br.jus.trf2.apolo.signer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;

import org.apache.pdfbox.pdmodel.PDDocument;

import com.crivano.swaggerservlet.SwaggerServlet;
import com.crivano.swaggerservlet.SwaggerUtils;

import br.jus.trf2.assijus.system.api.IAssijusSystem.DocIdHashGetRequest;
import br.jus.trf2.assijus.system.api.IAssijusSystem.DocIdHashGetResponse;
import br.jus.trf2.assijus.system.api.IAssijusSystem.IDocIdHashGet;

public class DocIdHashGet implements IDocIdHashGet {
	@Override
	public void run(DocIdHashGetRequest req, DocIdHashGetResponse resp) throws Exception {
		final boolean fForcePKCS7 = false;

		Id id = new Id(req.id);
		Extra extra = new Extra(null, 0);

		String sha1 = null;
		String sha256 = null;
		String status = null;
		String error = null;
		Integer pagecount = null;
		Timestamp dthrUltAtu = null;

		byte[] pdf = null;

		// Chama a procedure que recupera os dados do PDF para viabilizar a
		// assinatura
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

			// Retrieve parameters
			sha1 = cstmt.getString(6);
			sha256 = cstmt.getString(7);
			pagecount = cstmt.getInt(8);
			dthrUltAtu = cstmt.getTimestamp(9);

			extra.dthrultatu = dthrUltAtu;
			extra.pagecount = pagecount;

			// recupera o pdf para fazer assinatura sem política, apenas se ele
			// for diferente de null
			Blob blob = cstmt.getBlob(10);
			if (blob != null)
				pdf = blob.getBytes(1, (int) blob.length());
			// Temporariamente estamos recuperando o pdf e guardando no cache.
			// byte[] pdfCompressed = Utils.compress(pdf);
			// if (pdfCompressed == null)
			// throw new Exception("Não foi possível comprimir o PDF.");
			// Utils.store(sha1, pdfCompressed);

			status = cstmt.getString(11);
			error = cstmt.getString(12);
		} catch (Exception ex) {
			exception = ex;
			sha256 = null;
		} finally {
			if (cstmt != null)
				cstmt.close();
			if (conn != null)
				conn.close();
		}

		if (sha256 == null && ApoloSignerServlet.getProp("pdfservice.url") != null) {
			byte[] docCompressed = null;

			// Get documents from Oracle
			conn = null;
			PreparedStatement pstmt = null;
			ResultSet rset = null;
			try {
				conn = Utils.getConnection();
				pstmt = conn.prepareStatement(Utils.getSQL("doc"));
				pstmt.setInt(1, id.codsecao);
				pstmt.setLong(2, id.coddoc);
				pstmt.setTimestamp(3, id.dthrmov);
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

			// Utils.fileWrite("doc-compressed.doc", docCompressed);

			// Decompress
			byte[] doc = Utils.decompress(docCompressed);

			if (doc == null)
				throw new Exception("Não foi possível descomprimir o DOC.");

			// Utils.fileWrite("doc-uncompressed.doc", doc);

			// Convert
			pdf = Utils.convertDocToPdf(doc);

			if (pdf == null)
				throw new Exception("Não foi possível converter para PDF.");

			sha1 = SwaggerUtils.base64Encode(calcSha1(pdf));
			sha256 = SwaggerUtils.base64Encode(calcSha256(pdf));

			// Utils.fileWrite("pdf-uncompressed-nusad.pdf", pdf);

			byte[] pdfCompressed = Utils.compress(pdf);

			if (pdfCompressed == null)
				throw new Exception("Não foi possível comprimir o PDF.");

			// Utils.fileWrite("pdf-compressed.pdf", pdfCompressed);

			// Count the number of pages
			pagecount = PDDocument.load(pdf).getNumberOfPages();
			if (pagecount < 1)
				throw new Exception(
						"Não foi possível contar o número de páginas do PDF, provavelmente o documento está corrompido.");
			extra.pagecount = pagecount;

			SwaggerUtils.memCacheStore(sha1, pdfCompressed);
		}

		if (sha256 == null) {
			if (exception != null)
				throw exception;
			if (error == null)
				error = "SHA-256 inválido.";
			throw new Exception(error);
		}

		// Produce responses
		resp.sha1 = SwaggerUtils.base64Decode(sha1);
		resp.sha256 = SwaggerUtils.base64Decode(sha256);
		resp.extra = extra.toString();
		resp.secret = DocIdPdfGet.getSecret(id);

		// Force PKCS7
		if (fForcePKCS7) {
			resp.policy = "PKCS7";
			resp.doc = pdf;
		}
	}

	public static byte[] calcSha1(byte[] content) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.reset();
		md.update(content);
		byte[] output = md.digest();
		return output;
	}

	public static byte[] calcSha256(byte[] content) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.reset();
		md.update(content);
		byte[] output = md.digest();
		return output;
	}

	@Override
	public String getContext() {
		return "obter o hash";
	}
}
