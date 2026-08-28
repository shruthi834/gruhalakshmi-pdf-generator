import React from 'react';
import { FileText, ExternalLink, Download, RefreshCw, ZoomIn, Printer } from 'lucide-react';

export default function PdfViewerSection({
  previewUrl,
  previewLoading,
  onRefresh,
  onDownload,
  formData
}) {
  const handlePrint = () => {
    if (previewUrl) {
      const iframe = document.getElementById('pdf-preview-iframe');
      if (iframe && iframe.contentWindow) {
        iframe.contentWindow.print();
      } else {
        window.open(previewUrl, '_blank');
      }
    }
  };

  const handleOpenNewTab = () => {
    if (previewUrl) {
      window.open(previewUrl, '_blank');
    }
  };

  return (
    <div className="glass-card preview-pane">
      <div className="preview-toolbar">
        <div className="preview-title">
          <FileText size={18} color="var(--accent-gold)" />
          <span>Generated PDF Preview (ಮಂಜೂರಾತಿ ಪತ್ರ)</span>
        </div>

        {previewUrl && (
          <div className="preview-actions">
            <button
              type="button"
              className="icon-btn"
              onClick={onRefresh}
              title="Refresh Preview"
              disabled={previewLoading}
            >
              <RefreshCw size={16} className={previewLoading ? 'spin' : ''} />
            </button>
            <button
              type="button"
              className="icon-btn"
              onClick={handleOpenNewTab}
              title="Open in New Tab"
            >
              <ExternalLink size={16} />
            </button>
            <button
              id="download-preview-btn"
              type="button"
              className="icon-btn"
              onClick={onDownload}
              title="Download PDF"
              style={{ color: 'var(--accent-gold)' }}
            >
              <Download size={16} />
            </button>
          </div>
        )}
      </div>

      {previewLoading ? (
        <div className="empty-preview-placeholder">
          <div className="placeholder-icon">
            <span className="spinner" style={{ width: '28px', height: '28px' }}></span>
          </div>
          <h3>Rendering High-Fidelity PDF...</h3>
          <p>Dynamically injecting Date, Ration Card Number, and Name into the Karnataka Government template.</p>
        </div>
      ) : previewUrl ? (
        <iframe
          id="pdf-preview-iframe"
          className="pdf-viewer-frame"
          src={`${previewUrl}#toolbar=1&navpanes=0&scrollbar=1`}
          title="Generated PDF Preview"
        />
      ) : (
        <div className="empty-preview-placeholder">
          <div className="placeholder-icon">
            <FileText size={32} />
          </div>
          <h3>No PDF Generated Yet</h3>
          <p>
            Enter your Date, Ration Card Number, and Name on the left, then click <strong>"Live Preview PDF"</strong> or <strong>"Generate PDF"</strong>.
          </p>
        </div>
      )}
    </div>
  );
}
