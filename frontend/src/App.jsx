import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import FormCard from './components/FormCard';
import PdfViewerSection from './components/PdfViewerSection';
import { CheckCircle2, AlertCircle } from 'lucide-react';
import { transliterateToKannada, isKannadaText } from './utils/kannadaTransliterate';
import './App.css';

const EMPTY_FORM = {
  date: '',
  rationCardNumber: '',
  name: '',
};

export default function App() {
  const [formData, setFormData] = useState(EMPTY_FORM);
  const [paymentConfig, setPaymentConfig] = useState({ amount: 100, currency: 'INR', keyId: '' });
  const [paymentStage, setPaymentStage] = useState('idle'); // 'idle' | 'creating' | 'checkout' | 'verifying' | 'success' | 'cancelled' | 'failed'
  const [paymentError, setPaymentError] = useState(null);
  const [lastCapturedOrderId, setLastCapturedOrderId] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [toast, setToast] = useState(null);

  useEffect(() => {
    fetchPaymentConfig();
  }, []);

  const fetchPaymentConfig = async () => {
    try {
      const res = await fetch('/api/payment/config');
      if (res.ok) {
        const data = await res.json();
        setPaymentConfig(data);
      }
    } catch (err) {
      console.warn('Could not fetch payment config:', err);
    }
  };

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => {
      setToast(null);
    }, 4000);
  };

  const validateAndPreparePayload = () => {
    if (!formData.date || !formData.date.trim()) {
      showToast('Please select a Date (ದಿನಾಂಕ ಆಯ್ಕೆಮಾಡಿ)', 'error');
      return null;
    }

    if (!formData.rationCardNumber || !formData.rationCardNumber.trim()) {
      showToast('Please enter Ration Card Number (ಪಡಿತರ ಚೀಟಿ ಸಂಖ್ಯೆ ನಮೂದಿಸಿ)', 'error');
      return null;
    }

    if (!formData.name || !formData.name.trim()) {
      showToast('Please enter Beneficiary Name (ಹೆಸರನ್ನು ನಮೂದಿಸಿ)', 'error');
      return null;
    }

    let finalName = formData.name.trim();
    if (!isKannadaText(finalName)) {
      finalName = transliterateToKannada(finalName);
      setFormData((prev) => ({ ...prev, name: finalName }));
    }

    if (!finalName || !isKannadaText(finalName)) {
      showToast('Please enter a valid Kannada name (ಕನ್ನಡದಲ್ಲಿ ಹೆಸರು ನಮೂದಿಸಿ)', 'error');
      return null;
    }

    return {
      date: formData.date.trim(),
      rationCardNumber: formData.rationCardNumber.trim(),
      name: finalName,
    };
  };

  const handleProceedToPayment = async () => {
    const payload = validateAndPreparePayload();
    if (!payload) return;

    try {
      setPaymentStage('creating');
      setPaymentError(null);

      // 1. Create Razorpay order on backend
      const orderResponse = await fetch('/api/payment/create-order', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!orderResponse.ok) {
        const errData = await orderResponse.json().catch(() => ({}));
        throw new Error(errData.error || 'Unable to initiate payment. Please try again.');
      }

      const orderData = await orderResponse.json();
      const { orderId, amount, currency, keyId } = orderData;

      if (!window.Razorpay) {
        throw new Error('Razorpay SDK not loaded. Please check your internet connection.');
      }

      setPaymentStage('checkout');

      // 2. Open Razorpay Standard Checkout
      const options = {
        key: keyId || paymentConfig.keyId,
        amount: amount,
        currency: currency || 'INR',
        name: 'Karnataka Government',
        description: 'Gruha Lakshmi Sanction Order Fee',
        order_id: orderId,
        image: '/images/x6.png',
        handler: async function (response) {
          // 3. User paid -> verify payment on backend
          await verifyPaymentAndDownloadPdf({
            razorpayOrderId: response.razorpay_order_id || orderId,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
            rationCardNumber: payload.rationCardNumber
          });
        },
        modal: {
          ondismiss: function () {
            setPaymentStage('cancelled');
            showToast('Payment cancelled. No payment was completed.', 'error');
          }
        },
        prefill: {
          name: payload.name,
          contact: ''
        },
        theme: {
          color: '#e63946'
        }
      };

      const rzp = new window.Razorpay(options);
      rzp.on('payment.failed', function (response) {
        setPaymentStage('failed');
        setPaymentError(response.error?.description || 'Payment failed. Please try again.');
        showToast('Payment failed. Please try again.', 'error');
      });

      rzp.open();
    } catch (err) {
      console.error(err);
      setPaymentStage('failed');
      setPaymentError(err.message || 'Payment initiation failed');
      showToast(err.message || 'Payment initiation failed', 'error');
    }
  };

  const verifyPaymentAndDownloadPdf = async ({ razorpayOrderId, razorpayPaymentId, razorpaySignature, rationCardNumber }) => {
    try {
      setPaymentStage('verifying');

      const response = await fetch('/api/payment/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          razorpayOrderId,
          razorpayPaymentId,
          razorpaySignature
        }),
      });

      if (!response.ok) {
        const errData = await response.json().catch(() => ({}));
        throw new Error(errData.error || 'Payment verification failed');
      }

      // Received generated PDF from verified payment
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);

      // Trigger automatic download
      const link = document.createElement('a');
      link.href = url;
      link.download = `Gruhalakshmi_Sanction_Order_${rationCardNumber || 'order'}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
      setPreviewUrl(url);
      setLastCapturedOrderId(razorpayOrderId);
      setPaymentStage('success');

      showToast('Payment verified & PDF downloaded successfully! 🎉', 'success');
    } catch (err) {
      console.error('Payment verification error:', err);
      setPaymentStage('failed');
      setPaymentError(err.message || 'Payment verification failed. Please check with support.');
      showToast(err.message || 'Payment verification failed', 'error');
    }
  };

  const handleRetryDownload = async () => {
    if (!lastCapturedOrderId) return;
    try {
      setPaymentStage('verifying');
      const response = await fetch(`/api/payment/download/${lastCapturedOrderId}`);
      if (!response.ok) {
        throw new Error('Could not download PDF. Please try again.');
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);

      const link = document.createElement('a');
      link.href = url;
      link.download = `Gruhalakshmi_Sanction_Order_${formData.rationCardNumber || lastCapturedOrderId}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
      setPreviewUrl(url);
      setPaymentStage('success');
      showToast('PDF downloaded successfully!', 'success');
    } catch (err) {
      setPaymentStage('failed');
      setPaymentError(err.message);
      showToast(err.message, 'error');
    }
  };

  const handleReset = () => {
    setFormData(EMPTY_FORM);
    setPaymentStage('idle');
    setPaymentError(null);
    setLastCapturedOrderId(null);
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      setPreviewUrl(null);
    }
    showToast('Form cleared');
  };

  return (
    <div className="app-container">
      <Header />

      <main className="main-content">
        <div className="hero-banner">
          <h2>Gruha Lakshmi Sanction Order Generator</h2>
          <p>
            Official Karnataka Government Gruha Lakshmi Sanction Order Generator with instant Razorpay checkout.
          </p>
          <div className="kannada-tag">
            ಕರ್ನಾಟಕ ಸರ್ಕಾರ - ಗೃಹಲಕ್ಷ್ಮಿ ಯೋಜನೆ ಮಂಜೂರಾತಿ ಪತ್ರ
          </div>
        </div>

        <div className="app-grid">
          {/* Left Column: Form + Payment Checkout */}
          <FormCard
            formData={formData}
            setFormData={setFormData}
            onProceedToPayment={handleProceedToPayment}
            onReset={handleReset}
            paymentStage={paymentStage}
            paymentError={paymentError}
            paymentConfig={paymentConfig}
            onRetryDownload={handleRetryDownload}
            lastCapturedOrderId={lastCapturedOrderId}
          />

          {/* Right Column: PDF Preview (Only visible after verified payment) */}
          <PdfViewerSection
            previewUrl={previewUrl}
            previewLoading={paymentStage === 'verifying'}
            onRefresh={handleRetryDownload}
            onDownload={handleRetryDownload}
            formData={formData}
          />
        </div>
      </main>

      {/* Toast Notification */}
      {toast && (
        <div className="toast-container">
          <div className={`toast ${toast.type}`}>
            {toast.type === 'success' ? (
              <CheckCircle2 size={18} color="#06d6a0" />
            ) : (
              <AlertCircle size={18} color="#e63946" />
            )}
            <span>{toast.message}</span>
          </div>
        </div>
      )}

      <footer className="site-footer">
        <p>
          Karnataka Government Gruha Lakshmi PDF Generator • Secure Payments by Razorpay
        </p>
      </footer>
    </div>
  );
}
