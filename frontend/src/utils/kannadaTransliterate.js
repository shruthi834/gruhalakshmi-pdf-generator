// Complete & Robust Kannada Phonetic Transliteration Engine

const CONSONANTS = {
  // 3-letter combos
  'shh': '\u0CB7', 'chh': '\u0C9B',
  // 2-letter combos
  'kh': '\u0C96', 'gh': '\u0C98', 'ch': '\u0C9A', 'jh': '\u0C9D',
  'th': '\u0CA4', 'dh': '\u0CA6', 'ph': '\u0CAB', 'bh': '\u0CAD',
  'sh': '\u0CB6', 'ng': '\u0C99', 'ny': '\u0C9E',
  // Single letter consonants
  'k': '\u0C95', 'g': '\u0C97', 'c': '\u0C9A', 'j': '\u0C9C',
  't': '\u0CA4', 'd': '\u0CA6', 'n': '\u0CA8', 'p': '\u0CAA',
  'f': '\u0CAB', 'b': '\u0CAC', 'm': '\u0CAE', 'y': '\u0CAF',
  'r': '\u0CB0', 'l': '\u0CB2', 'v': '\u0CB5', 'w': '\u0CB5',
  's': '\u0CB8', 'h': '\u0CB9'
};

const INDEPENDENT_VOWELS = {
  'aa': '\u0C86', 'a': '\u0C85',
  'ee': '\u0C88', 'ii': '\u0C88', 'i': '\u0C87',
  'oo': '\u0C8A', 'uu': '\u0C8A', 'u': '\u0C89',
  'ea': '\u0C8F', 'e': '\u0C8E',
  'ai': '\u0C90',
  'o': '\u0C92', 'au': '\u0C94', 'ou': '\u0C94',
  'am': '\u0C85\u0C82', 'ah': '\u0C85\u0C83'
};

const MATRAS = {
  'aa': '\u0CBE', 'a': '',
  'ee': '\u0CC0', 'ii': '\u0CC0', 'i': '\u0CBF',
  'oo': '\u0CC2', 'uu': '\u0CC2', 'u': '\u0CC1',
  'ea': '\u0CC7', 'e': '\u0CC6',
  'ai': '\u0CC8',
  'o': '\u0CCA',
  'au': '\u0CCC', 'ou': '\u0CCC'
};

const VIRAMA = '\u0CCD'; // ್

const POPULAR_NAMES = {
  'shivamma': 'ಶಿವಮ್ಮ',
  'sivamma': 'ಶಿವಮ್ಮ',
  'shivam': 'ಶಿವಂ',
  'lakshmamma': 'ಲಕ್ಷ್ಮಮ್ಮ',
  'laxmamma': 'ಲಕ್ಷ್ಮಮ್ಮ',
  'lakshmi': 'ಲಕ್ಷ್ಮಿ',
  'laxmi': 'ಲಕ್ಷ್ಮಿ',
  'bhagyamma': 'ಭಾಗ್ಯಮ್ಮ',
  'bhagya': 'ಭಾಗ್ಯ',
  'sunitha': 'ಸುನಿತಾ',
  'sunita': 'ಸುನಿತಾ',
  'kavya': 'ಕಾವ್ಯ',
  'parvathi': 'ಪಾರ್ವತಿ',
  'parvati': 'ಪಾರ್ವತಿ',
  'shruthi': 'ಶ್ರುತಿ',
  'shruti': 'ಶ್ರುತಿ',
  'sanju': 'ಸಂಜು',
  'sanjay': 'ಸಂಜಯ್',
  'manjula': 'ಮಂಜುಳಾ',
  'radha': 'ರಾಧಾ',
  'geetha': 'ಗೀತಾ',
  'geeta': 'ಗೀತಾ',
  'shobha': 'ಶೋಭಾ',
  'renuka': 'ರೇಣುಕಾ',
  'kamala': 'ಕಮಲಾ',
  'suma': 'ಸುಮಾ',
  'sarojamma': 'ಸರೋಜಮ್ಮ',
  'saroja': 'ಸರೋಜಾ',
  'gangamma': 'ಗಂಗಮ್ಮ',
  'gowramma': 'ಗೌರಮ್ಮ',
  'basamma': 'ಬಸಮ್ಮ',
  'savithri': 'ಸಾವಿತ್ರಿ',
  'savitha': 'ಸವಿತಾ',
  'anita': 'ಅನಿತಾ',
  'anitha': 'ಅನಿತಾ',
  'pushpa': 'ಪುಷ್ಪ',
  'rekha': 'ರೇಖಾ',
  'roopa': 'ರೂಪ',
  'rupa': 'ರೂಪ',
  'deepa': 'ದೀಪಾ',
  'jyothi': 'ಜ್ಯೋತಿ',
  'jyoti': 'ಜ್ಯೋತಿ',
  'padma': 'ಪದ್ಮ',
  'shanta': 'ಶಾಂತಾ',
  'shanthi': 'ಶಾಂತಿ',
  'mangala': 'ಮಂಗಳಾ',
  'meenakshi': 'ಮೀನಾಕ್ಷಿ',
  'sumithra': 'ಸುಮಿತ್ರಾ',
  'kusuma': 'ಕುಸುಮ',
  'nagaveni': 'ನಾಗವೇಣಿ',
  'bhavani': 'ಭವಾನಿ',
  'vani': 'ವಾಣಿ',
  'uma': 'ಉಮಾ',
  'nethra': 'ನೇತ್ರಾ',
  'rashmi': 'ರಶ್ಮಿ',
  'priya': 'ಪ್ರಿಯಾ',
  'pooja': 'ಪೂಜಾ',
  'asha': 'ಆಶಾ',
  'usha': 'ಉಷಾ',
  'girija': 'ಗಿರಿಜಾ',
  'jayamma': 'ಜಯಮ್ಮ',
  'jaya': 'ಜಯಾ',
  'rathna': 'ರತ್ನ',
  'ratna': 'ರತ್ನ',
  'lalitha': 'ಲಲಿತಾ',
  'devaki': 'ದೇವಕಿ',
  'sujatha': 'ಸುಜಾತಾ',
  'shailaja': 'ಶೈಲಜಾ',
  'kamalamma': 'ಕಮಲಮ್ಮ',
  'susheela': 'ಸುಶೀಲಾ',
  'prema': 'ಪ್ರೇಮ'
};

export function hasEnglishLetters(text) {
  if (!text) return false;
  return /[a-zA-Z]/.test(text);
}

export function isPureKannadaText(text) {
  if (!text || !text.trim()) return false;
  const hasKannada = /[\u0C80-\u0CFF]/.test(text);
  const hasLatin = /[a-zA-Z]/.test(text);
  return hasKannada && !hasLatin;
}

export function isKannadaText(text) {
  if (!text || !text.trim()) return false;
  return isPureKannadaText(text);
}

export function transliterateToKannada(text) {
  if (!text || !text.trim()) return '';

  // If already native Kannada without any English letters, keep completely untouched
  if (!hasEnglishLetters(text)) {
    return text.trim();
  }

  const words = text.split(/\s+/);
  return words.map((w) => transliterateWord(w)).join(' ');
}

function transliterateWord(word) {
  if (!word) return '';
  const trimmed = word.trim();

  // If this specific word has no English letters, keep as is
  if (!hasEnglishLetters(trimmed)) {
    return trimmed;
  }

  const lower = trimmed.toLowerCase();

  // 1. Direct dictionary lookup
  if (POPULAR_NAMES[lower]) {
    return POPULAR_NAMES[lower];
  }

  let out = '';
  let i = 0;
  const len = lower.length;

  while (i < len) {
    const origChar = trimmed[i];
    const code = origChar.charCodeAt(0);
    // If character is already native Kannada Unicode, preserve it
    if ((code >= 0x0C80 && code <= 0x0CFF) || code === 32 || code === 44 || code === 46) {
      out += origChar;
      i++;
      continue;
    }

    // Match consonants (longest prefix: 3, 2, 1)
    let cMatch = null;
    let cLen = 0;
    for (const cl of [3, 2, 1]) {
      if (i + cl <= len) {
        const sub = lower.substring(i, i + cl);
        if (CONSONANTS[sub]) {
          cMatch = CONSONANTS[sub];
          cLen = cl;
          break;
        }
      }
    }

    if (cMatch) {
      i += cLen;
      // Match following vowel (longest: 2, 1)
      let vMatch = null;
      let vLen = 0;
      for (const vl of [2, 1]) {
        if (i + vl <= len) {
          const vsub = lower.substring(i, i + vl);
          if (MATRAS.hasOwnProperty(vsub)) {
            vMatch = MATRAS[vsub];
            vLen = vl;
            break;
          }
        }
      }

      if (vMatch !== null) {
        out += cMatch + vMatch;
        i += vLen;
      } else {
        out += cMatch + VIRAMA;
      }
    } else {
      // Independent vowel check
      let ivMatch = null;
      let ivLen = 0;
      for (const vl of [2, 1]) {
        if (i + vl <= len) {
          const sub = lower.substring(i, i + vl);
          if (INDEPENDENT_VOWELS[sub]) {
            ivMatch = INDEPENDENT_VOWELS[sub];
            ivLen = vl;
            break;
          }
        }
      }

      if (ivMatch) {
        out += ivMatch;
        i += ivLen;
      } else {
        out += lower[i];
        i++;
      }
    }
  }

  return out.replace(/\u0CCD$/, '');
}
