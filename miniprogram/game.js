const GRAVITY = 0.5;
const FRICTION = 0.99;
const WALL_RESTITUTION = 0.05;
const GROUND_RESTITUTION = 0.15;
const BALL_RESTITUTION = 0.05;
const MIN_GROUND_BOUNCE = 0.3;
const MERGE_SPEED_LIMIT = 1.5;
const MERGE_DISTANCE_TOLERANCE = 2.0;

const SPAWN_WEIGHTS = [4, 3, 2, 1];
const TOP_MARGIN = 15;
const RED_LINE_Y_RATIO = 80 / 600;
const SOLVER_ITERATIONS = 5;
const SETTLE_VELOCITY = 0.4;
const QUIESCENT_FRAMES_REQUIRED = 3;
const COLLISION_THRESHOLD = 0.1;
const MAX_TURN_FRAMES = 90;
const MAX_STATIC_SHARE = 0.15;

const BASE_RADII = [20, 40, 60, 70, 100, 120, 140];
const COLORS = [
  '#FF69B4',
  '#FFA500',
  '#FFD700',
  '#8B5A2B',
  '#8A2BE2',
  '#00BFFF',
  '#FFD700'
];
const DENSITIES = [1.0, 1.0, 1.0, 1.0, 1.0, 1.2, 0.8];

let WIDTH = 600;
let HEIGHT = 600;
let RADII = [...BASE_RADII];
let RED_LINE_Y = 80;
let SCALE = 1;

class Ball {
  constructor(x, y, level) {
    this.x = x;
    this.y = y;
    this.level = level;
    this.vx = 0;
    this.vy = 0;
    this.markedForRemoval = false;
    this.merging = false;
    this.mergeDrawScale = 1.0;
  }

  static getMaxLevel() {
    return RADII.length - 1;
  }

  static getRadiusForLevel(level) {
    return RADII[level];
  }

  getRadius() {
    return RADII[this.level];
  }

  getLevel() {
    return this.level;
  }

  getX() { return this.x; }
  getY() { return this.y; }
  getVx() { return this.vx; }
  getVy() { return this.vy; }

  setX(x) { this.x = x; }
  setY(y) { this.y = y; }
  setVx(vx) { this.vx = vx; }
  setVy(vy) { this.vy = vy; }

  getColor() {
    return COLORS[this.level];
  }

  getDensity() {
    return DENSITIES[this.level];
  }

  getMass() {
    const r = this.getRadius();
    return Math.PI * r * r * this.getDensity();
  }

  isMarkedForRemoval() { return this.markedForRemoval; }
  markForRemoval() { this.markedForRemoval = true; }

  isMerging() { return this.merging; }
  setMerging(merging) { this.merging = merging; }

  setMergeDrawScale(scale) { this.mergeDrawScale = scale; }

  canMergeWith(other) {
    return this.level === other.level && this.level < Ball.getMaxLevel();
  }

  getMergeScore() {
    return (this.level + 1) * 10;
  }

  isUltimateLevel() {
    return this.level === Ball.getMaxLevel();
  }

  draw(ctx) {
    const r = Math.floor(this.getRadius() * this.mergeDrawScale);
    const cx = Math.floor(this.x);
    const cy = Math.floor(this.y);

    if (this.isUltimateLevel()) {
      this.drawUltimateBall(ctx, cx, cy, r);
    } else if (this.level === 5) {
      this.drawDolphin(ctx, cx, cy, r);
    } else {
      ctx.fillStyle = this.getColor();
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.fill();
      let strokeColor = this.adjustColor(this.getColor(), -50);
      if (this.level === 4) strokeColor = '#4B0082';
      ctx.strokeStyle = strokeColor;
      ctx.lineWidth = Math.max(2, r * 0.05);
      ctx.stroke();
      this.drawAnimal(ctx, cx, cy, r);
    }
  }

  drawAnimal(ctx, cx, cy, r) {
    switch (this.level) {
      case 0: this.drawCat(ctx, cx, cy, r); break;
      case 1: this.drawDog(ctx, cx, cy, r); break;
      case 2: this.drawRabbit(ctx, cx, cy, r); break;
      case 3: this.drawBear(ctx, cx, cy, r); break;
      case 4: this.drawDragon(ctx, cx, cy, r); break;
    }
  }

  drawCat(ctx, cx, cy, r) {
    ctx.fillStyle = '#FFFFFF';
    ctx.beginPath(); ctx.ellipse(cx - r / 3, cy - r + (r/3)/2, r / 4, r / 6, 0, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.ellipse(cx + r / 12, cy - r + (r/3)/2, r / 4, r / 6, 0, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#FF69B4';
    ctx.beginPath(); ctx.ellipse(cx - r / 4, cy - r + (r/4)/2 + r / 6, r / 6, r / 8, 0, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.ellipse(cx + r / 8, cy - r + (r/4)/2 + r / 6, r / 6, r / 8, 0, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#000000';
    ctx.beginPath(); ctx.arc(cx - r / 4, cy - r / 6, r / 8, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(cx + r / 8, cy - r / 6, r / 8, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#FFFFFF';
    ctx.beginPath(); ctx.arc(cx - r / 5, cy - r / 5, r / 16, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(cx + r / 6, cy - r / 5, r / 16, 0, Math.PI * 2); ctx.fill();
    ctx.strokeStyle = '#000000';
    ctx.lineWidth = 1;
    ctx.beginPath(); ctx.moveTo(cx - r / 4, cy - r / 8); ctx.lineTo(cx - r * 1.2, cy - r / 4); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(cx + r / 4, cy - r / 8); ctx.lineTo(cx + r * 1.2, cy - r / 4); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(cx - r / 4, cy); ctx.lineTo(cx - r * 1.2, cy); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(cx + r / 4, cy); ctx.lineTo(cx + r * 1.2, cy); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(cx - r / 4, cy + r / 8); ctx.lineTo(cx - r * 1.2, cy + r / 4); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(cx + r / 4, cy + r / 8); ctx.lineTo(cx + r * 1.2, cy + r / 4); ctx.stroke();
  }

  drawDog(ctx, cx, cy, r) {
    ctx.fillStyle = '#FFFFFF';
    ctx.beginPath(); ctx.ellipse(cx - r / 3, cy - r * 0.7 + (r/2.5)/2, r / 5, r / 3, -0.2, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.ellipse(cx + r / 6, cy - r * 0.7 + (r/2.5)/2, r / 5, r / 3, 0.2, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#D2B48C';
    ctx.beginPath(); ctx.ellipse(cx - r / 3, cy - r * 0.65 + (r/3.5)/2, r / 7, r / 4, -0.2, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.ellipse(cx + r / 6, cy - r * 0.65 + (r/3.5)/2, r / 7, r / 4, 0.2, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#000000';
    ctx.beginPath(); ctx.arc(cx - r / 4, cy - r / 5, r / 8, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(cx + r / 8, cy - r / 5, r / 8, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#333333';
    ctx.beginPath(); ctx.arc(cx, cy - r / 15, r / 10, 0, Math.PI * 2); ctx.fill();
    ctx.strokeStyle = '#000000';
    ctx.lineWidth = 2;
    ctx.beginPath(); ctx.ellipse(cx, cy + r / 8, r / 4, r / 8, 0, 0, Math.PI); ctx.stroke();
  }

  drawRabbit(ctx, cx, cy, r) {
    ctx.fillStyle = '#FFFFFF';
    ctx.beginPath(); ctx.ellipse(cx - r / 3, cy - r * 0.85 + (r/3)/2, r / 5, r / 3, 0, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.ellipse(cx + r / 6, cy - r * 0.85 + (r/3)/2, r / 5, r / 3, 0, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#FFB4C8';
    ctx.beginPath(); ctx.ellipse(cx - r / 3, cy - r * 0.75 + (r/4)/2, r / 7, r / 5, 0, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.ellipse(cx + r / 6, cy - r * 0.75 + (r/4)/2, r / 7, r / 5, 0, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#000000';
    ctx.beginPath(); ctx.arc(cx - r / 4, cy - r / 6, r / 8, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(cx + r / 8, cy - r / 6, r / 8, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#FFB4C8';
    ctx.beginPath(); ctx.ellipse(cx - r / 3, cy + r / 5 + (r/8)/2, r / 6, r / 16, 0, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.ellipse(cx + r / 6, cy + r / 5 + (r/8)/2, r / 6, r / 16, 0, 0, Math.PI * 2); ctx.fill();
  }

  drawBear(ctx, cx, cy, r) {
    ctx.fillStyle = '#D2B48C';
    ctx.beginPath(); ctx.arc(cx - r / 2, cy - r / 2, r / 3, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(cx + r / 6, cy - r / 2, r / 3, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#000000';
    ctx.beginPath(); ctx.arc(cx - r / 4, cy - r / 6, r / 8, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(cx + r / 8, cy - r / 6, r / 8, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#FFFFFF';
    ctx.beginPath(); ctx.arc(cx - r / 5, cy - r / 5, r / 16, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(cx + r / 6, cy - r / 5, r / 16, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#333333';
    ctx.beginPath(); ctx.arc(cx, cy, r / 10, 0, Math.PI * 2); ctx.fill();
    ctx.strokeStyle = '#000000';
    ctx.lineWidth = 2;
    ctx.beginPath(); ctx.moveTo(cx, cy + r / 15); ctx.lineTo(cx - r / 12, cy + r / 6); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(cx, cy + r / 15); ctx.lineTo(cx + r / 12, cy + r / 6); ctx.stroke();
  }

  drawDragon(ctx, cx, cy, r) {
    ctx.fillStyle = '#FF0000';
    ctx.beginPath(); ctx.ellipse(cx - r / 2, cy - r / 2 + (r/2)/2, r / 4, r / 4, 0, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.ellipse(cx + r / 4, cy - r / 2 + (r/2)/2, r / 4, r / 4, 0, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#FFC800';
    ctx.beginPath(); ctx.ellipse(cx - r / 3, cy - r / 3 + (r/3)/2, r / 6, r / 6, 0, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.ellipse(cx + r / 6, cy - r / 3 + (r/3)/2, r / 6, r / 6, 0, 0, Math.PI * 2); ctx.fill();
    ctx.strokeStyle = '#FFD700';
    ctx.lineWidth = 2;
    ctx.beginPath(); ctx.moveTo(cx - r / 4, cy - r / 6); ctx.lineTo(cx - r / 2, cy - r / 3); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(cx + r / 8, cy - r / 6); ctx.lineTo(cx + r / 2, cy - r / 3); ctx.stroke();
    ctx.fillStyle = '#000000';
    ctx.beginPath(); ctx.arc(cx - r / 4, cy - r / 5, r / 8, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(cx + r / 8, cy - r / 5, r / 8, 0, Math.PI * 2); ctx.fill();
    ctx.strokeStyle = '#FFC800';
    ctx.beginPath(); ctx.ellipse(cx - r / 2, cy, r / 2, r / 4, 0, 0, Math.PI); ctx.stroke();
    ctx.beginPath(); ctx.ellipse(cx - r / 2, cy, r / 2, r / 4, 0, Math.PI, Math.PI * 2); ctx.stroke();
  }

  drawDolphin(ctx, cx, cy, r) {
    ctx.fillStyle = '#00BFFF';
    ctx.beginPath(); ctx.arc(cx, cy, r, 0, Math.PI * 2); ctx.fill();
    ctx.strokeStyle = '#0087CE';
    ctx.lineWidth = Math.max(2, r * 0.05);
    ctx.stroke();
    ctx.fillStyle = '#FFFFFF';
    ctx.beginPath(); ctx.arc(cx - r / 4, cy - r / 4, r / 3, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#000000';
    ctx.beginPath(); ctx.arc(cx - r / 5, cy - r / 5, r / 8, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#FFFFFF';
    ctx.beginPath(); ctx.arc(cx - r / 6, cy - r / 4, r / 16, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = '#FFFFFF';
    ctx.beginPath(); ctx.ellipse(cx - r / 4, cy - r / 3 + (r/2)/2, r / 4, r / 4, 0, Math.PI, Math.PI * 1.5); ctx.fill();
    ctx.fillStyle = '#0087CE';
    ctx.beginPath(); ctx.ellipse(cx + r / 2, cy - r / 4 + (r/3)/2, r / 4, r / 6, 0, 0, Math.PI * 2); ctx.fill();
  }

  drawUltimateBall(ctx, cx, cy, r) {
    ctx.fillStyle = '#FFD700';
    ctx.beginPath(); ctx.arc(cx, cy, r, 0, Math.PI * 2); ctx.fill();
    ctx.strokeStyle = '#DAA520';
    ctx.lineWidth = Math.max(2, r * 0.05);
    ctx.stroke();
    ctx.fillStyle = 'rgba(255, 255, 255, 0.4)';
    ctx.beginPath(); ctx.ellipse(cx, cy - r / 4, r / 2, r * 0.3, 0, 0, Math.PI * 2); ctx.fill();
    this.drawStar(ctx, cx, cy, 5, r * 0.5, r * 0.25, '#FFA500');
  }

  drawStar(ctx, cx, cy, numPoints, outerRadius, innerRadius, color) {
    ctx.beginPath();
    const angleStep = Math.PI / numPoints;
    for (let i = 0; i < 2 * numPoints; i++) {
      const radius = (i % 2 === 0) ? outerRadius : innerRadius;
      const angle = i * angleStep - Math.PI / 2;
      const x = cx + Math.cos(angle) * radius;
      const y = cy + Math.sin(angle) * radius;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.closePath();
    ctx.fillStyle = color;
    ctx.fill();
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.6)';
    ctx.lineWidth = 2;
    ctx.stroke();
  }

  adjustColor(color, amount) {
    const hex = color.replace('#', '');
    const r = Math.max(0, Math.min(255, parseInt(hex.substr(0, 2), 16) + amount));
    const g = Math.max(0, Math.min(255, parseInt(hex.substr(2, 2), 16) + amount));
    const b = Math.max(0, Math.min(255, parseInt(hex.substr(4, 2), 16) + amount));
    return `rgb(${r}, ${g}, ${b})`;
  }
}

class MergeAnimation {
  constructor(a, b) {
    this.ballA = a;
    this.ballB = b;
    this.startAX = a.getX();
    this.startAY = a.getY();
    this.startBX = b.getX();
    this.startBY = b.getY();
    this.targetX = (this.startAX + this.startBX) / 2;
    this.targetY = (this.startAY + this.startBY) / 2;
    this.resultLevel = a.getLevel() + 1;
    this.frame = 0;
    this.TOTAL_FRAMES = 3;
    a.setMerging(true);
    b.setMerging(true);
  }

  update() {
    this.frame++;
    const t = Math.min(1.0, this.frame / this.TOTAL_FRAMES);
    const eased = 1 - Math.pow(1 - t, 3);

    this.ballA.setX(this.startAX + (this.targetX - this.startAX) * eased);
    this.ballA.setY(this.startAY + (this.targetY - this.startAY) * eased);
    this.ballB.setX(this.startBX + (this.targetX - this.startBX) * eased);
    this.ballB.setY(this.startBY + (this.targetY - this.startBY) * eased);

    const squash = 1.0 + 0.16 * Math.sin(t * Math.PI);
    this.ballA.setMergeDrawScale(squash);
    this.ballB.setMergeDrawScale(squash);

    return this.frame >= this.TOTAL_FRAMES;
  }

  createResultBall() {
    const x = (this.ballA.getX() + this.ballB.getX()) / 2;
    const y = (this.ballA.getY() + this.ballB.getY()) / 2;
    const merged = new Ball(x, y, this.resultLevel);
    merged.setVy(0);
    merged.setVx(0);
    return merged;
  }

  finish() {
    this.ballA.markForRemoval();
    this.ballB.markForRemoval();
    this.ballA.setMerging(false);
    this.ballB.setMerging(false);
    this.ballA.setMergeDrawScale(1.0);
    this.ballB.setMergeDrawScale(1.0);
  }

  involves(ball) {
    return ball === this.ballA || ball === this.ballB;
  }
}

let canvas;
let ctx;
let balls = [];
let mergeAnimations = [];
let previewBall = null;
let watchedBall = null;
let score = 0;
let turnInProgress = false;
let quiescentFrames = 0;
let turnFrames = 0;
let mergeOccurredThisFrame = false;
let gameOver = false;
let showCongrats = false;
let previewX = WIDTH / 2;
let audioContext = null;
let gameLoopId = null;

function initAudio() {
  try {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    audioContext = new AudioContext();
  } catch (e) {
    console.log('AudioContext not supported');
  }
}

function playReleaseSound() {
  if (!audioContext) return;
  try {
    const ctx = audioContext;
    const oscillator = ctx.createOscillator();
    const gainNode = ctx.createGain();
    oscillator.connect(gainNode);
    gainNode.connect(ctx.destination);

    const now = ctx.currentTime;
    oscillator.type = 'sine';
    oscillator.frequency.setValueAtTime(400, now);
    oscillator.frequency.exponentialRampToValueAtTime(200, now + 0.15);

    gainNode.gain.setValueAtTime(0.3, now);
    gainNode.gain.exponentialRampToValueAtTime(0.01, now + 0.15);

    oscillator.start(now);
    oscillator.stop(now + 0.15);
  } catch (e) {
    console.log('play release sound failed:', e);
  }
}

function playMergeSound() {
  if (!audioContext) return;
  try {
    const ctx = audioContext;
    const oscillator = ctx.createOscillator();
    const gainNode = ctx.createGain();
    oscillator.connect(gainNode);
    gainNode.connect(ctx.destination);

    const now = ctx.currentTime;
    oscillator.type = 'sine';
    oscillator.frequency.setValueAtTime(300, now);
    oscillator.frequency.exponentialRampToValueAtTime(700, now + 0.2);

    gainNode.gain.setValueAtTime(0.4, now);
    gainNode.gain.exponentialRampToValueAtTime(0.01, now + 0.2);

    oscillator.start(now);
    oscillator.stop(now + 0.2);
  } catch (e) {
    console.log('play merge sound failed:', e);
  }
}

function getPreviewY(radius) {
  return Math.max(TOP_MARGIN * SCALE + radius, RED_LINE_Y + radius);
}

function getNextBallLevel() {
  const total = SPAWN_WEIGHTS.reduce((a, b) => a + b, 0);
  let rand = Math.random() * total;
  for (let i = 0; i < SPAWN_WEIGHTS.length; i++) {
    rand -= SPAWN_WEIGHTS[i];
    if (rand <= 0) return i;
  }
  return 0;
}

function prepareNextPreview() {
  const level = getNextBallLevel();
  const r = Ball.getRadiusForLevel(level);
  previewBall = new Ball(previewX, getPreviewY(r), level);
}

function isPhysicsActive(ball) {
  if (ball.isMarkedForRemoval() || ball.isMerging()) {
    return false;
  }
  for (let i = 0; i < mergeAnimations.length; i++) {
    if (mergeAnimations[i].involves(ball)) {
      return false;
    }
  }
  return true;
}

function applyGravity() {
  for (let i = 0; i < balls.length; i++) {
    const b = balls[i];
    if (!isPhysicsActive(b)) continue;
    b.setVy(b.getVy() + GRAVITY);
    b.setVx(b.getVx() * FRICTION);
    b.setX(b.getX() + b.getVx());
    b.setY(b.getY() + b.getVy());
  }
}

function resolveWallCollisions() {
  for (let i = 0; i < balls.length; i++) {
    const b = balls[i];
    if (!isPhysicsActive(b)) continue;

    const r = b.getRadius();
    const x = b.getX();
    const y = b.getY();

    if (x - r < 0) {
      b.setX(r);
      b.setVx(-b.getVx() * WALL_RESTITUTION);
    } else if (x + r > WIDTH) {
      b.setX(WIDTH - r);
      b.setVx(-b.getVx() * WALL_RESTITUTION);
    }

    if (y - r < 0) {
      b.setY(r);
      b.setVy(-b.getVy() * WALL_RESTITUTION);
    } else if (y + r > HEIGHT) {
      b.setY(HEIGHT - r);
      if (b.getVy() > 0) {
        bounceGround(b);
      }
    }
  }
}

function bounceGround(ball) {
  const bouncedVy = -ball.getVy() * GROUND_RESTITUTION;
  if (Math.abs(bouncedVy) < MIN_GROUND_BOUNCE) {
    ball.setVy(0);
  } else {
    ball.setVy(bouncedVy);
  }
  ball.setVx(ball.getVx() * FRICTION);
}

function resolveBallCollisions() {
  for (let i = 0; i < balls.length; i++) {
    const a = balls[i];
    if (!isPhysicsActive(a)) continue;

    for (let j = i + 1; j < balls.length; j++) {
      const b = balls[j];
      if (!isPhysicsActive(b)) continue;

      const dx = b.getX() - a.getX();
      const dy = b.getY() - a.getY();
      const dist = Math.sqrt(dx * dx + dy * dy);
      const minDist = a.getRadius() + b.getRadius();

      if (dist >= minDist + COLLISION_THRESHOLD || dist === 0) continue;
      if (a.canMergeWith(b)) continue;

      const aMotion = Math.hypot(a.getVx(), a.getVy());
      const bMotion = Math.hypot(b.getVx(), b.getVy());
      const overlap = minDist - dist;

      if (aMotion < 0.1 && bMotion < 0.1 && overlap < 0.5) continue;

      separateBalls(a, b, overlap, dx, dy, dist);
      applyCollisionImpulse(a, b, dx, dy, dist);
    }
  }
}

function separateBalls(a, b, overlap, dx, dy, dist) {
  const nx = dx / dist;
  const ny = dy / dist;

  const aMotion = Math.hypot(a.getVx(), a.getVy());
  const bMotion = Math.hypot(b.getVx(), b.getVy());

  const massA = a.getMass();
  const massB = b.getMass();
  const totalMass = massA + massB;

  const slop = 0.5;
  const percent = 0.8;
  const correction = Math.max(overlap - slop, 0) * percent;

  let aShare, bShare;

  if (aMotion < SETTLE_VELOCITY && bMotion < SETTLE_VELOCITY) {
    if (overlap < 0.8) return;
    aShare = 0.1;
    bShare = 0.1;
  } else if (aMotion < SETTLE_VELOCITY) {
    aShare = 0.05;
    bShare = 0.95;
  } else if (bMotion < SETTLE_VELOCITY) {
    bShare = 0.05;
    aShare = 0.95;
  } else {
    aShare = massB / totalMass;
    bShare = massA / totalMass;
  }

  a.setX(a.getX() - nx * correction * aShare);
  a.setY(a.getY() - ny * correction * aShare);
  b.setX(b.getX() + nx * correction * bShare);
  b.setY(b.getY() + ny * correction * bShare);
}

function applyCollisionImpulse(a, b, dx, dy, dist) {
  const nx = dx / dist;
  const ny = dy / dist;

  const dvx = a.getVx() - b.getVx();
  const dvy = a.getVy() - b.getVy();
  const dot = dvx * nx + dvy * ny;

  if (dot <= 0) return;

  const massA = a.getMass();
  const massB = b.getMass();

  const impulse = (1 + BALL_RESTITUTION) * dot / (1.0 / massA + 1.0 / massB);

  a.setVx(a.getVx() - impulse * nx / massA);
  a.setVy(a.getVy() - impulse * ny / massA);
  b.setVx(b.getVx() + impulse * nx / massB);
  b.setVy(b.getVy() + impulse * ny / massB);
}

function snapToRest() {
  for (let i = 0; i < balls.length; i++) {
    const ball = balls[i];
    if (!isPhysicsActive(ball)) continue;

    const speed = Math.hypot(ball.getVx(), ball.getVy());
    if (speed >= SETTLE_VELOCITY) continue;

    const restY = findRestY(ball);
    const diff = ball.getY() - restY;

    if (diff < -10.0) continue;

    if (Math.abs(diff) <= 1.5) {
      ball.setVy(0);
      ball.setVx(ball.getVx() * 0.3);
      ball.setY(restY);
    } else if (diff > 1.5 && diff <= 10.0) {
      ball.setY(ball.getY() - diff * 0.1);
      ball.setVx(ball.getVx() * 0.5);
    }
  }
}

function findRestY(ball) {
  let restY = HEIGHT - ball.getRadius();
  for (let i = 0; i < balls.length; i++) {
    const other = balls[i];
    if (other === ball || other.isMarkedForRemoval() || other.isMerging()) continue;

    const dx = ball.getX() - other.getX();
    const sumR = ball.getRadius() + other.getRadius();

    if (Math.abs(dx) >= sumR + 0.5) continue;

    const dy = Math.sqrt(sumR * sumR - dx * dx);
    const candidateY = other.getY() - dy;

    if (candidateY < restY) restY = candidateY;
  }
  return restY;
}

function resolveMerges() {
  for (let i = 0; i < balls.length; i++) {
    const a = balls[i];
    if (!isPhysicsActive(a)) continue;

    for (let j = i + 1; j < balls.length; j++) {
      const b = balls[j];
      if (!isPhysicsActive(b)) continue;

      if (!canMergeNow(a, b)) continue;

      startMergeAnimation(a, b);
      return;
    }
  }
}

function canMergeNow(a, b) {
  if (!a.canMergeWith(b)) return false;

  const dx = b.getX() - a.getX();
  const dy = b.getY() - a.getY();
  const dist = Math.sqrt(dx * dx + dy * dy);
  const minDist = a.getRadius() + b.getRadius();

  const overlap = minDist - dist;
  
  if (overlap < -MERGE_DISTANCE_TOLERANCE) return false;

  const aSpeed = Math.hypot(a.getVx(), a.getVy());
  const bSpeed = Math.hypot(b.getVx(), b.getVy());
  
  if (overlap >= 2.0) {
    if (aSpeed >= MERGE_SPEED_LIMIT * 2 || bSpeed >= MERGE_SPEED_LIMIT * 2) return false;
  } else {
    if (aSpeed >= MERGE_SPEED_LIMIT || bSpeed >= MERGE_SPEED_LIMIT) return false;
  }

  return true;
}

function startMergeAnimation(a, b) {
  if (a.isMerging() || b.isMerging()) return;
  for (let i = 0; i < mergeAnimations.length; i++) {
    if (mergeAnimations[i].involves(a) || mergeAnimations[i].involves(b)) return;
  }

  mergeAnimations.push(new MergeAnimation(a, b));
  playMergeSound();
  mergeOccurredThisFrame = true;
  quiescentFrames = 0;
}

function removeMergedBalls() {
  balls = balls.filter(b => !b.isMarkedForRemoval());
}

function checkRedLineCollision() {
  for (let i = 0; i < balls.length; i++) {
    const b = balls[i];
    if (b.isMarkedForRemoval() || b.isMerging()) continue;
    if (b.getY() - b.getRadius() < RED_LINE_Y) {
      gameOver = true;
      break;
    }
  }
}

function isBallSupported(ball) {
  if (ball.isMerging()) return true;

  let restY = HEIGHT - ball.getRadius();
  for (let i = 0; i < balls.length; i++) {
    const other = balls[i];
    if (other === ball || other.isMarkedForRemoval() || other.isMerging()) continue;

    const dx = ball.getX() - other.getX();
    const sumR = ball.getRadius() + other.getRadius();

    if (Math.abs(dx) >= sumR + 0.5) continue;

    const dy = Math.sqrt(sumR * sumR - dx * dx);
    const candidateY = other.getY() - dy;

    if (candidateY < restY) restY = candidateY;
  }

  const diff = ball.getY() - restY;
  return diff >= -2.0 && diff <= 2.0;
}

function isBallSettled(ball) {
  const speed = Math.hypot(ball.getVx(), ball.getVy());
  if (speed >= SETTLE_VELOCITY) return false;
  return isBallSupported(ball);
}

function isSceneQuiescent() {
  if (mergeOccurredThisFrame || mergeAnimations.length > 0) return false;

  for (let i = 0; i < balls.length; i++) {
    const ball = balls[i];
    if (ball.isMarkedForRemoval() || ball.isMerging()) continue;
    if (!isBallSettled(ball)) return false;
  }
  return true;
}

function updateTurnState() {
  if (!turnInProgress) return;

  if (watchedBall != null && (watchedBall.isMarkedForRemoval() || !balls.includes(watchedBall))) {
    watchedBall = null;
  }

  if (turnFrames >= MAX_TURN_FRAMES) {
    forceEndTurn();
    return;
  }

  if (isSceneQuiescent()) {
    quiescentFrames++;
    if (quiescentFrames >= QUIESCENT_FRAMES_REQUIRED) {
      turnInProgress = false;
      watchedBall = null;
      if (previewBall == null) prepareNextPreview();
    }
  } else {
    quiescentFrames = 0;
  }
}

function forceEndTurn() {
  turnInProgress = false;
  watchedBall = null;
  quiescentFrames = 0;
  turnFrames = 0;
  if (previewBall == null) prepareNextPreview();
}

function releasePreviewBall() {
  if (turnInProgress || gameOver || showCongrats) return;
  if (previewBall == null) return;

  const radius = previewBall.getRadius();
  const x = previewBall.getX();
  const y = previewBall.getY();

  for (let i = 0; i < balls.length; i++) {
    const ball = balls[i];
    if (ball.isMarkedForRemoval() || ball.isMerging()) continue;
    const dx = ball.getX() - x;
    const dy = ball.getY() - y;
    const dist = Math.sqrt(dx * dx + dy * dy);
    if (dist < ball.getRadius() + radius) return;
  }

  watchedBall = new Ball(x, y, previewBall.getLevel());
  balls.push(watchedBall);
  playReleaseSound();
  previewBall = null;
  turnInProgress = true;
  quiescentFrames = 0;
  turnFrames = 0;
}

function restartGame() {
  balls = [];
  mergeAnimations = [];
  previewBall = null;
  watchedBall = null;
  score = 0;
  turnInProgress = false;
  quiescentFrames = 0;
  turnFrames = 0;
  mergeOccurredThisFrame = false;
  gameOver = false;
  showCongrats = false;
  previewX = WIDTH / 2;
  prepareNextPreview();
}

function updateMergeAnimations() {
  const finished = [];
  for (let i = 0; i < mergeAnimations.length; i++) {
    if (mergeAnimations[i].update()) finished.push(mergeAnimations[i]);
  }

  for (let i = 0; i < finished.length; i++) {
    const anim = finished[i];
    const merged = anim.createResultBall();
    anim.finish();
    balls.push(merged);
    score += merged.getMergeScore();
    playMergeSound();

    if (merged.isUltimateLevel()) {
      showCongrats = true;
    }

    if (anim.involves(watchedBall)) {
      watchedBall = merged;
    }

    mergeOccurredThisFrame = true;
    quiescentFrames = 0;

    const idx = mergeAnimations.indexOf(anim);
    if (idx > -1) mergeAnimations.splice(idx, 1);
  }
}

function update() {
  if (gameOver || showCongrats) return;

  mergeOccurredThisFrame = false;

  updateMergeAnimations();
  applyGravity();

  for (let i = 0; i < SOLVER_ITERATIONS; i++) {
    resolveWallCollisions();
    resolveBallCollisions();
  }

  snapToRest();
  resolveMerges();
  removeMergedBalls();
  checkRedLineCollision();

  if (turnInProgress) turnFrames++;
  updateTurnState();
}

function drawBackground() {
  const gradient = ctx.createLinearGradient(0, 0, 0, HEIGHT);
  gradient.addColorStop(0, '#87CEEB');
  gradient.addColorStop(1, '#E0F6FF');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, WIDTH, HEIGHT);

  ctx.fillStyle = 'rgba(255, 255, 255, 0.7)';
  ctx.beginPath();
  ctx.arc(100 * SCALE, 80 * SCALE, 30 * SCALE, 0, Math.PI * 2);
  ctx.arc(130 * SCALE, 90 * SCALE, 25 * SCALE, 0, Math.PI * 2);
  ctx.arc(70 * SCALE, 95 * SCALE, 20 * SCALE, 0, Math.PI * 2);
  ctx.fill();

  ctx.beginPath();
  ctx.arc(WIDTH - 100 * SCALE, 60 * SCALE, 40 * SCALE, 0, Math.PI * 2);
  ctx.arc(WIDTH - 60 * SCALE, 70 * SCALE, 35 * SCALE, 0, Math.PI * 2);
  ctx.arc(WIDTH - 130 * SCALE, 80 * SCALE, 30 * SCALE, 0, Math.PI * 2);
  ctx.fill();

  ctx.beginPath();
  ctx.arc(WIDTH / 2, 150 * SCALE, 25 * SCALE, 0, Math.PI * 2);
  ctx.arc(WIDTH / 2 + 25 * SCALE, 160 * SCALE, 20 * SCALE, 0, Math.PI * 2);
  ctx.fill();
}

function drawRedLine() {
  ctx.strokeStyle = '#FF4444';
  ctx.lineWidth = 3 * SCALE;
  ctx.setLineDash([10 * SCALE, 5 * SCALE]);
  ctx.beginPath();
  ctx.moveTo(0, RED_LINE_Y);
  ctx.lineTo(WIDTH, RED_LINE_Y);
  ctx.stroke();
  ctx.setLineDash([]);

  ctx.fillStyle = 'rgba(255, 68, 68, 0.15)';
  ctx.fillRect(0, TOP_MARGIN * SCALE, WIDTH, RED_LINE_Y - TOP_MARGIN * SCALE);
}

function drawRoundRect(x, y, width, height, radius) {
  ctx.moveTo(x + radius, y);
  ctx.lineTo(x + width - radius, y);
  ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
  ctx.lineTo(x + width, y + height - radius);
  ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
  ctx.lineTo(x + radius, y + height);
  ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
  ctx.lineTo(x, y + radius);
  ctx.quadraticCurveTo(x, y, x + radius, y);
  ctx.closePath();
}

function drawCongratsOverlay() {
  ctx.fillStyle = 'rgba(255, 215, 0, 0.3)';
  ctx.fillRect(0, 0, WIDTH, HEIGHT);

  ctx.fillStyle = '#FFD700';
  ctx.strokeStyle = '#DAA520';
  ctx.lineWidth = 4 * SCALE;
  ctx.beginPath();
  drawRoundRect(WIDTH / 2 - 150 * SCALE, HEIGHT / 2 - 120 * SCALE, 300 * SCALE, 240 * SCALE, 20 * SCALE);
  ctx.fill();
  ctx.stroke();

  ctx.fillStyle = '#FFFFFF';
  ctx.font = 'bold ' + (48 * SCALE) + 'px Arial';
  ctx.textAlign = 'center';
  ctx.fillText('恭喜！', WIDTH / 2, HEIGHT / 2 - 50 * SCALE);

  ctx.font = (28 * SCALE) + 'px Arial';
  ctx.fillStyle = '#333333';
  ctx.fillText('成功合成终极球！', WIDTH / 2, HEIGHT / 2);

  ctx.font = 'bold ' + (36 * SCALE) + 'px Arial';
  ctx.fillStyle = '#FF6600';
  ctx.fillText('得分: ' + score, WIDTH / 2, HEIGHT / 2 + 60 * SCALE);

  ctx.font = (24 * SCALE) + 'px Arial';
  ctx.fillStyle = '#666666';
  ctx.fillText('点击屏幕继续游戏', WIDTH / 2, HEIGHT / 2 + 110 * SCALE);
}

function drawScore() {
  ctx.fillStyle = '#FFFFFF';
  ctx.font = 'bold ' + (32 * SCALE) + 'px Arial';
  ctx.textAlign = 'right';
  ctx.textShadow = '2px 2px 4px rgba(0, 0, 0, 0.5)';
  ctx.fillText('得分: ' + score, WIDTH - 20 * SCALE, 40 * SCALE);
}

function drawHint(text) {
  ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
  ctx.font = (24 * SCALE) + 'px Arial';
  ctx.textAlign = 'center';
  ctx.textShadow = '1px 1px 2px rgba(0, 0, 0, 0.5)';
  ctx.fillText(text, WIDTH / 2, HEIGHT - 40 * SCALE);
}

function render() {
  if (!ctx) return;

  ctx.clearRect(0, 0, WIDTH, HEIGHT);
  drawBackground();
  drawRedLine();

  for (let i = 0; i < balls.length; i++) {
    balls[i].draw(ctx);
  }

  if (previewBall) {
    ctx.globalAlpha = 0.7;
    previewBall.draw(ctx);
    ctx.globalAlpha = 1.0;
  }

  drawScore();
  
  if (gameOver) {
    drawHint('游戏结束！点击重新开始');
  } else if (showCongrats) {
    drawCongratsOverlay();
  } else {
    drawHint('移动手指调整位置，松开释放圆球');
  }
}

function gameLoop() {
  update();
  render();
  gameLoopId = requestAnimationFrame(gameLoop);
}

function handleTouchStart(e) {
  if (showCongrats) {
    showCongrats = false;
    return;
  }
}

function handleTouchMove(e) {
  if (turnInProgress || gameOver || showCongrats) return;

  const touch = e.touches[0];
  const touchX = touch.x || touch.clientX || touch.pageX;
  const r = previewBall.getRadius();
  previewX = Math.max(r, Math.min(WIDTH - r, touchX));
  previewBall.setX(previewX);
}

function handleTouchEnd() {
  if (showCongrats) {
    showCongrats = false;
    return;
  }
  
  if (gameOver) {
    restartGame();
    return;
  }
  
  releasePreviewBall();
}

function init() {
  canvas = wx.createCanvas();
  ctx = canvas.getContext('2d');
  
  const sysInfo = wx.getSystemInfoSync();
  WIDTH = sysInfo.windowWidth;
  HEIGHT = sysInfo.windowHeight;
  SCALE = WIDTH / 600;
  RADII = BASE_RADII.map(r => r * SCALE);
  RED_LINE_Y = HEIGHT * RED_LINE_Y_RATIO;
  previewX = WIDTH / 2;

  initAudio();
  prepareNextPreview();

  wx.onTouchStart(handleTouchStart);
  wx.onTouchMove(handleTouchMove);
  wx.onTouchEnd(handleTouchEnd);

  gameLoop();
}

init();