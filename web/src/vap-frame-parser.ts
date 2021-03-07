/*
 * Tencent is pleased to support the open source community by making vap available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
export default class FrameParser {
  constructor(source, headData) {
    this.config = source || {};
    this.headData = headData;
    this.frame = [];
    this.textureMap = {}
  }

  private config;
  private headData;
  private frame;
  private textureMap;
  private canvas:HTMLCanvasElement;
  private ctx:CanvasRenderingContext2D | null;
  private srcData;

  async init() {
    this.initCanvas();
    // 判断是url还是json对象
    if(/\/\/[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]\.json$/.test(this.config)){
      this.config = await this.getConfigBySrc(this.config);
    }
    await this.parseSrc(this.config);
    this.canvas.parentNode.removeChild(this.canvas);
    this.frame = this.config.frame || [];
    return this;
  }

  initCanvas() {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    canvas.style.display = 'none';
    document.body.appendChild(canvas);
    this.ctx = ctx;
    this.canvas = canvas;
  }

  loadImg(url:string) {
    return new Promise((resolve, reject) => {
      // console.log('load img:', url)
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = function() {
        resolve(this);
      };
      img.onerror = function(e) {
        console.error('frame 资源加载失败:' + url);
        reject(new Error('frame 资源加载失败:' + url));
      };
      img.src = url;
    })
  }

  parseSrc(dataJson) {
    const src = (this.srcData = {});
    console.warn("dataJson="+ dataJson)
    return Promise.all(
      (dataJson.src || []).map(async item => {
        item.img = null;
        console.warn("item.srcTag="+item.srcTag)
        if (!this.headData[item.srcTag.slice(1, item.srcTag.length-1)]) {
          console.warn(`vap: 融合信息没有传入123：${item.srcTag}`);
        } else {
          if (item.srcType === 'txt') {
            item.textStr = item.srcTag.replace(/\[(.*)\]/, ($0, $1) => {
              return this.headData[$1];
            });
            item.img = this.makeTextImg(item);
          } else if (item.srcType === 'img') {
            item.imgUrl = item.srcTag.replace(/\[(.*)\]/, ($0, $1) => {
              return this.headData[$1]
            });
            try {
              item.img = await this.loadImg(item.imgUrl + '?t=' + Date.now());
            } catch (e) {}
          }
          if (item.img) {
            src[item.srcId] = item;
          }
        }
      })
    )
  }

  /**
   * 下载json文件
   * @param jsonUrl json外链
   * @returns {Promise}
   */
  getConfigBySrc(jsonUrl:string) {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open("GET", jsonUrl, true);
      xhr.responseType = "json";
      xhr.onload = function() {
        if (xhr.status === 200 || xhr.status === 304 && xhr.response) {
          const res = xhr.response;
          resolve(res);
        } else {
          reject(new Error("http response invalid" + xhr.status));
        }
      };
      xhr.send();
    });
  }

  /**
   * 文字转换图片
   * @param {*} param0
   */
  makeTextImg({ textStr, w, h, color, style }) {
    const ctx = this.ctx;
    ctx.canvas.width = w;
    ctx.canvas.height = h;
    const fontSize = Math.min(w / textStr.length, h - 8); // 需留一定间隙
    const font = [`${fontSize}px`, 'Arial'];
    if (style === 'b') {
      font.unshift('bold');
    }
    ctx.font = font.join(' ');
    ctx.textBaseline = 'middle';
    ctx.textAlign = 'center';
    ctx.fillStyle = color;
    ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
    ctx.fillText(textStr, w / 2, h / 2);
    // console.log('frame : ' + textStr, ctx.canvas.toDataURL('image/png'))
    return ctx.getImageData(0, 0, w, h);
  }
  getFrame(frame) {
    return this.frame.find(item => {
      return item.i === frame;
    })
  }
}
