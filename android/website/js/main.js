document.addEventListener('DOMContentLoaded', () => {
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    setTimeout(() => {
        document.body.classList.remove('no-transition');
    }, 120);

    const yearSpan = document.getElementById('current-year');
    if (yearSpan) {
        yearSpan.textContent = new Date().getFullYear();
    }

    setupCopyButtons();
    setupRevealObserver();
    setupHeroCookie(prefersReducedMotion);
});

function setupCopyButtons() {
    document.querySelectorAll('[data-copy]').forEach((button) => {
        button.addEventListener('click', async () => {
            const selector = button.getAttribute('data-copy');
            const target = selector ? document.querySelector(selector) : null;
            if (!target) return;

            const text = ('value' in target && target.value)
                ? target.value.trim()
                : target.textContent.trim();
            const feedback = button.closest('.contact-block')?.querySelector('.copy-feedback')
                || document.getElementById('copy-feedback');

            try {
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(text);
                } else {
                    const range = document.createRange();
                    range.selectNodeContents(target);
                    const selection = window.getSelection();
                    selection.removeAllRanges();
                    selection.addRange(range);
                    document.execCommand('copy');
                    selection.removeAllRanges();
                }
                if (feedback) feedback.textContent = 'Copied to clipboard.';
            } catch {
                if (feedback) feedback.textContent = 'Select the address above and copy manually.';
            }
        });
    });
}

function setupRevealObserver() {
    const revealElements = document.querySelectorAll('.reveal');
    if (!revealElements.length) return;

    const revealOnScroll = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                revealOnScroll.unobserve(entry.target);
            }
        });
    }, {
        threshold: 0.12,
        rootMargin: '0px 0px -48px 0px',
    });

    revealElements.forEach((element) => revealOnScroll.observe(element));
}

function setupHeroCookie(prefersReducedMotion) {
    const stage = document.querySelector('[data-cookie-stage]');
    if (!stage) return;

    const messageNode = stage.querySelector('[data-cookie-message]');
    const crumbs = Array.from(stage.querySelectorAll('.crumb'));
    const triggers = Array.from(document.querySelectorAll('[data-hero-trigger]'));
    const hero = document.querySelector('.hero');
    // Website-only demo phrases — intentionally separate from the app message pack.
    const messages = [
        'May your next pause feel a little softer',
        'Carry a quiet smile into the evening',
        'Let tonight end a bit lighter than it began',
        'Keep one gentle thought close today',
        'Hope finds you in the smallest places too',
    ];

    // Android: crumbs begin at CrackMomentMs + 18 (~740ms).
    // size is for tight-cropped PNGs (not full 1024×1536 canvases).
    // endX/endY = rest offsets from center; launch* = mid-flight apex.
    const CRACK_MS = 740;
    const OPEN_MS = 760;
    const SETTLE_MS = 1450;
    const crumbScenarios = [
        [
            { launchX: -0.12, launchY: -0.06, endX: -0.26, endY: 0.28, r1: -84, r2: -40, s: 1.05, size: 0.038, d: 0 },
            { launchX: -0.08, launchY: -0.03, endX: -0.2, endY: 0.31, r1: -48, r2: -22, s: 0.9, size: 0.03, d: 16 },
            { launchX: -0.05, launchY: -0.08, endX: -0.14, endY: 0.33, r1: -96, r2: -58, s: 0.78, size: 0.024, d: 30 },
            { launchX: -0.02, launchY: -0.05, endX: -0.06, endY: 0.3, r1: 16, r2: 4, s: 0.94, size: 0.032, d: 44 },
            { launchX: 0.01, launchY: -0.04, endX: 0.02, endY: 0.32, r1: 24, r2: 10, s: 0.8, size: 0.022, d: 54 },
            { launchX: 0.05, launchY: -0.07, endX: 0.12, endY: 0.3, r1: 88, r2: 40, s: 0.84, size: 0.024, d: 20 },
            { launchX: 0.08, launchY: -0.04, endX: 0.18, endY: 0.29, r1: 56, r2: 26, s: 0.96, size: 0.028, d: 36 },
            { launchX: 0.12, launchY: -0.02, endX: 0.26, endY: 0.27, r1: 34, r2: 16, s: 1.02, size: 0.036, d: 10 },
            { launchX: 0.1, launchY: -0.06, endX: 0.3, endY: 0.26, r1: 110, r2: 60, s: 0.76, size: 0.022, d: 28 },
            { launchX: 0.02, launchY: -0.03, endX: 0.05, endY: 0.34, r1: 18, r2: 8, s: 0.7, size: 0.018, d: 48 },
            { launchX: -0.14, launchY: -0.02, endX: -0.32, endY: 0.25, r1: -30, r2: -16, s: 0.74, size: 0.02, d: 8 },
            { launchX: 0.14, launchY: -0.02, endX: 0.32, endY: 0.24, r1: 38, r2: 20, s: 0.76, size: 0.02, d: 24 },
            { launchX: 0, launchY: -0.05, endX: -0.01, endY: 0.35, r1: 12, r2: 2, s: 0.68, size: 0.016, d: 40 },
        ],
        [
            { launchX: -0.14, launchY: -0.05, endX: -0.3, endY: 0.26, r1: -100, r2: -48, s: 1.12, size: 0.04, d: 0 },
            { launchX: -0.1, launchY: -0.02, endX: -0.26, endY: 0.3, r1: -56, r2: -26, s: 0.94, size: 0.032, d: 14 },
            { launchX: -0.06, launchY: -0.08, endX: -0.16, endY: 0.32, r1: -112, r2: -68, s: 0.8, size: 0.026, d: 32 },
            { launchX: -0.03, launchY: -0.04, endX: -0.1, endY: 0.34, r1: -14, r2: -4, s: 0.96, size: 0.03, d: 46 },
            { launchX: 0, launchY: -0.02, endX: 0.01, endY: 0.31, r1: 20, r2: 8, s: 0.82, size: 0.024, d: 56 },
            { launchX: 0.04, launchY: -0.06, endX: 0.1, endY: 0.3, r1: 68, r2: 30, s: 0.86, size: 0.022, d: 22 },
            { launchX: 0.08, launchY: -0.04, endX: 0.2, endY: 0.28, r1: 48, r2: 22, s: 0.92, size: 0.028, d: 38 },
            { launchX: 0.12, launchY: -0.02, endX: 0.28, endY: 0.27, r1: 28, r2: 14, s: 0.98, size: 0.034, d: 12 },
            { launchX: 0.11, launchY: -0.05, endX: 0.32, endY: 0.25, r1: 92, r2: 48, s: 0.78, size: 0.024, d: 30 },
            { launchX: 0.03, launchY: -0.03, endX: 0.06, endY: 0.33, r1: 22, r2: 10, s: 0.7, size: 0.018, d: 50 },
            { launchX: -0.16, launchY: -0.01, endX: -0.34, endY: 0.24, r1: -36, r2: -16, s: 0.72, size: 0.02, d: 8 },
            { launchX: 0.16, launchY: 0, endX: 0.34, endY: 0.23, r1: 44, r2: 20, s: 0.72, size: 0.02, d: 24 },
            { launchX: -0.01, launchY: -0.05, endX: 0.02, endY: 0.35, r1: 30, r2: 10, s: 0.66, size: 0.016, d: 42 },
        ],
        [
            { launchX: -0.11, launchY: -0.07, endX: -0.24, endY: 0.29, r1: -88, r2: -44, s: 1.0, size: 0.036, d: 0 },
            { launchX: -0.07, launchY: -0.03, endX: -0.16, endY: 0.31, r1: -40, r2: -18, s: 0.86, size: 0.028, d: 18 },
            { launchX: -0.03, launchY: -0.05, endX: -0.08, endY: 0.33, r1: -70, r2: -28, s: 0.88, size: 0.026, d: 34 },
            { launchX: 0.01, launchY: -0.03, endX: 0.04, endY: 0.32, r1: 16, r2: 6, s: 0.76, size: 0.02, d: 48 },
            { launchX: 0.04, launchY: -0.07, endX: 0.1, endY: 0.3, r1: 80, r2: 34, s: 0.82, size: 0.022, d: 58 },
            { launchX: 0.07, launchY: -0.04, endX: 0.16, endY: 0.3, r1: 50, r2: 22, s: 0.9, size: 0.028, d: 22 },
            { launchX: 0.11, launchY: -0.02, endX: 0.26, endY: 0.27, r1: 24, r2: 12, s: 1.02, size: 0.034, d: 12 },
            { launchX: 0.13, launchY: -0.05, endX: 0.3, endY: 0.26, r1: 108, r2: 54, s: 0.78, size: 0.022, d: 28 },
            { launchX: 0.15, launchY: -0.02, endX: 0.33, endY: 0.24, r1: 40, r2: 16, s: 0.72, size: 0.02, d: 40 },
            { launchX: 0, launchY: -0.04, endX: -0.02, endY: 0.34, r1: -12, r2: -2, s: 0.66, size: 0.016, d: 52 },
            { launchX: -0.13, launchY: -0.02, endX: -0.3, endY: 0.26, r1: -26, r2: -12, s: 0.74, size: 0.02, d: 8 },
            { launchX: 0.09, launchY: -0.06, endX: 0.2, endY: 0.3, r1: 90, r2: 42, s: 0.84, size: 0.024, d: 36 },
            { launchX: 0.03, launchY: -0.02, endX: 0.07, endY: 0.33, r1: 18, r2: 8, s: 0.68, size: 0.018, d: 44 },
        ],
    ];

    let isAnimating = false;
    let isOpen = false;
    let currentScenario = -1;
    let sequenceTimers = [];

    clearCrumbs();
    setTriggerLabels(getOpenLabel());
    updateStageLabel();
    applyParallax();

    triggers.forEach((trigger) => {
        trigger.addEventListener('click', (event) => {
            event.preventDefault();
            handleHeroAction();
        });
    });

    stage.addEventListener('click', handleHeroAction);
    stage.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            handleHeroAction();
        }
    });

    function handleHeroAction() {
        if (isAnimating) return;

        const stageBounds = stage.getBoundingClientRect();
        const isOffscreen = stageBounds.top < 0 || stageBounds.bottom > window.innerHeight;

        if (isOffscreen && hero) {
            hero.scrollIntoView({ behavior: 'smooth', block: 'start' });
            window.setTimeout(() => {
                if (isOpen) resetHeroCookie();
                else runHeroSequence();
            }, 260);
            return;
        }

        if (isOpen) {
            resetHeroCookie();
            return;
        }

        runHeroSequence();
    }

    function schedule(fn, ms) {
        const id = window.setTimeout(fn, ms);
        sequenceTimers.push(id);
        return id;
    }

    function clearSequenceTimers() {
        sequenceTimers.forEach((id) => window.clearTimeout(id));
        sequenceTimers = [];
    }

    function resetHeroCookie() {
        isAnimating = true;
        clearSequenceTimers();
        clearCrumbs();
        stage.classList.remove('is-breaking', 'is-open', 'is-settled');
        void stage.offsetWidth;
        isOpen = false;
        isAnimating = false;
        updateStageLabel();
        setTriggerLabels(getOpenLabel());
    }

    function runHeroSequence() {
        if (isAnimating || isOpen) return;
        isAnimating = true;

        clearSequenceTimers();
        clearCrumbs();
        stage.classList.remove('is-breaking', 'is-open', 'is-settled');

        currentScenario = nextScenarioIndex(currentScenario, crumbScenarios.length);
        const scenario = crumbScenarios[currentScenario];
        const nextMessage = messages[Math.floor(Math.random() * messages.length)];
        if (messageNode) messageNode.textContent = nextMessage;
        setTriggerLabels('Opening...');

        // Force reflow so removing/adding classes restarts CSS transitions cleanly.
        void stage.offsetWidth;
        stage.classList.add('is-breaking');

        if (prefersReducedMotion) {
            schedule(() => {
                stage.classList.remove('is-breaking');
                stage.classList.add('is-open', 'is-settled');
                settleCrumbsImmediately(scenario);
                isOpen = true;
                updateStageLabel();
                setTriggerLabels(getResetLabel());
                isAnimating = false;
            }, 180);
            return;
        }

        // Crumbs only after the crack moment — not during the press/pre-crack.
        schedule(() => {
            animateCrumbs(scenario);
        }, CRACK_MS);

        schedule(() => {
            stage.classList.add('is-open');
            stage.classList.remove('is-breaking');
        }, OPEN_MS);

        schedule(() => {
            stage.classList.add('is-settled');
            isOpen = true;
            updateStageLabel();
            setTriggerLabels(getResetLabel());
            isAnimating = false;
        }, SETTLE_MS);
    }

    function clearCrumbs() {
        crumbs.forEach((crumb) => {
            crumb.getAnimations().forEach((animation) => animation.cancel());
            crumb.hidden = true;
            crumb.style.visibility = 'hidden';
            crumb.style.opacity = '0';
            crumb.style.width = '';
            crumb.style.marginLeft = '';
            crumb.style.marginTop = '';
            crumb.style.transform = 'translate(0px, 0px) rotate(0deg) scale(1)';
        });
    }

    function stageMetrics() {
        const rect = stage.getBoundingClientRect();
        const minSide = Math.min(rect.width, rect.height);
        return {
            width: rect.width,
            height: rect.height,
            minSide,
        };
    }

    function prepareCrumb(crumb, sizePx) {
        crumb.hidden = false;
        crumb.style.visibility = 'visible';
        crumb.style.width = `${sizePx}px`;
        crumb.style.marginLeft = `${-sizePx / 2}px`;
        crumb.style.marginTop = `${-sizePx / 2}px`;
        crumb.style.opacity = '0';
        crumb.style.transform = 'translate(0px, 0px) rotate(0deg) scale(0.7)';
    }

    function settleCrumbsImmediately(scenario) {
        const { width, height, minSide } = stageMetrics();
        scenario.forEach((spec, index) => {
            const crumb = crumbs[index];
            if (!crumb) return;
            const sizePx = Math.max(8, minSide * spec.size);
            const endX = width * spec.endX;
            const endY = height * (spec.endY + 0.16);
            prepareCrumb(crumb, sizePx);
            crumb.style.opacity = '1';
            crumb.style.transform = `translate(${endX}px, ${endY}px) rotate(${spec.r2}deg) scale(${spec.s})`;
        });
    }

    function animateCrumbs(scenario) {
        const { width, height, minSide } = stageMetrics();

        scenario.forEach((spec, index) => {
            const crumb = crumbs[index];
            if (!crumb) return;

            const sizePx = Math.max(8, minSide * spec.size);
            const startX = width * spec.launchX * 0.15;
            const startY = height * spec.launchY * 0.15;
            const launchX = width * spec.launchX;
            const launchY = height * spec.launchY;
            const endX = width * spec.endX;
            // Extra drop: web stage keeps crumbs mid-cookie at Android touchY alone.
            const endY = height * (spec.endY + 0.16);

            prepareCrumb(crumb, sizePx);

            const animation = crumb.animate([
                {
                    opacity: 0,
                    transform: `translate(${startX}px, ${startY}px) rotate(0deg) scale(${spec.s * 0.7})`,
                    offset: 0,
                },
                {
                    opacity: 1,
                    transform: `translate(${launchX}px, ${launchY}px) rotate(${spec.r1}deg) scale(${spec.s})`,
                    offset: 0.28,
                },
                {
                    opacity: 1,
                    transform: `translate(${endX}px, ${endY - minSide * 0.03}px) rotate(${spec.r2 + 6}deg) scale(${spec.s})`,
                    offset: 0.72,
                },
                {
                    opacity: 0.95,
                    transform: `translate(${endX}px, ${endY}px) rotate(${spec.r2}deg) scale(${spec.s})`,
                    offset: 1,
                },
            ], {
                duration: 900,
                delay: spec.d,
                easing: 'cubic-bezier(0.2, 0.82, 0.24, 1)',
                fill: 'forwards',
            });

            animation.finished.then(() => {
                try {
                    animation.commitStyles();
                    animation.cancel();
                } catch {
                    // Older browsers may not support commitStyles.
                }
                crumb.style.opacity = '0.95';
                crumb.style.transform = `translate(${endX}px, ${endY}px) rotate(${spec.r2}deg) scale(${spec.s})`;
            }).catch(() => {
                // Animation was cancelled during reset — ignore.
            });
        });
    }

    function applyParallax() {
        if (prefersReducedMotion) return;

        stage.addEventListener('pointermove', (event) => {
            const rect = stage.getBoundingClientRect();
            const x = ((event.clientX - rect.left) / rect.width) - 0.5;
            const y = ((event.clientY - rect.top) / rect.height) - 0.5;
            stage.style.setProperty('--parallax-x', `${x * -18}px`);
            stage.style.setProperty('--parallax-y', `${y * -14}px`);
        });

        stage.addEventListener('pointerleave', () => {
            stage.style.setProperty('--parallax-x', '0px');
            stage.style.setProperty('--parallax-y', '0px');
        });
    }

    function setTriggerLabels(label) {
        triggers.forEach((trigger) => {
            trigger.textContent = label;
        });
    }

    function getOpenLabel() {
        return triggers[0]?.dataset.labelOpen || 'Open Cookie';
    }

    function getResetLabel() {
        return triggers[0]?.dataset.labelReset || 'Open another';
    }

    function updateStageLabel() {
        stage.setAttribute(
            'aria-label',
            isOpen ? 'Reset the fortune cookie' : 'Open the fortune cookie',
        );
    }
}

function nextScenarioIndex(currentIndex, total) {
    if (total <= 1) return 0;
    if (currentIndex < 0 || currentIndex >= total) return Math.floor(Math.random() * total);

    const next = Math.floor(Math.random() * (total - 1));
    return next >= currentIndex ? next + 1 : next;
}
