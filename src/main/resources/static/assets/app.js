const state={token:localStorage.getItem('cc_token'),user:null,listPage:1,reviewPage:1,filters:{keyword:'',language:'',status:'',errorType:''},authNotice:''};
const $=s=>document.querySelector(s),$$=s=>[...document.querySelectorAll(s)];
const pageNames={overview:'学习总览',questions:'我的错题',questionForm:'记录错题',questionDetail:'错题详情',analysis:'AI分析',history:'分析历史',knowledge:'知识点地图',stats:'学习统计',reviews:'复习计划'};

function icons(){if(window.lucide)window.lucide.createIcons({attrs:{'stroke-width':1.75,'aria-hidden':'true'}})}
function syncThemeControls(){const dark=document.documentElement.dataset.theme==='dark';$$('.theme-toggle').forEach(button=>{button.innerHTML=`<i data-lucide="${dark?'sun':'moon'}"></i>`;button.setAttribute('aria-label',dark?'切换到浅色模式':'切换到深色模式');button.title=dark?'切换到浅色模式':'切换到深色模式'});icons()}
function toggleTheme(){const next=document.documentElement.dataset.theme==='dark'?'light':'dark';document.documentElement.dataset.theme=next;localStorage.setItem('cc_theme',next);syncThemeControls();toast(next==='dark'?'已切换到深色模式':'已切换到浅色模式','success')}
function esc(v){return String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function attr(v){return esc(v).replace(/\n/g,'&#10;')}
function fmtDate(v){return v?new Date(v).toLocaleDateString('zh-CN',{year:'numeric',month:'2-digit',day:'2-digit'}):'暂无日期'}
function fmtTime(v){return v?new Date(v).toLocaleString('zh-CN',{month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'}):'暂无日期'}
function statusMeta(s){return s===2?['已分析','analyzed','circle-check']:s===3?['分析失败','failed','circle-alert']:s===1?['分析中','running','loader-circle']:['待分析','pending','clock-3']}
function route(){const path=(location.hash.replace(/^#\/?/,'')||'overview').split('/').filter(Boolean);if(['login','register','forgot-password'].includes(path[0]))return{view:path[0]};if(path[0]==='questions'&&path[1]==='new')return{view:'questionForm'};if(path[0]==='questions'&&path[1]&&path[2]==='edit')return{view:'questionForm',id:Number(path[1])};if(path[0]==='questions'&&path[1]&&path[2]==='analysis')return{view:'analysis',id:Number(path[1])};if(path[0]==='questions'&&path[1]&&path[2]==='history')return{view:'history',id:Number(path[1])};if(path[0]==='questions'&&path[1])return{view:'questionDetail',id:Number(path[1])};return{view:['overview','questions','knowledge','stats','reviews'].includes(path[0])?path[0]:'overview'}}
function go(path){location.hash=`#/${path}`}

async function api(path,options={}){const headers={'Content-Type':'application/json',...(options.headers||{})};if(state.token)headers.Authorization=`Bearer ${state.token}`;const response=await fetch(`/api${path}`,{...options,headers});let body={};try{body=await response.json()}catch{}if(response.status===401&&state.token&&!path.startsWith('/user/login')&&!path.startsWith('/user/password/')){logout();throw new Error('登录状态已过期')}if(!response.ok||(body.code&&body.code!==200))throw new Error(body.message||'请求失败');return body.data}
function toast(message,type='info'){const el=$('#toast');el.className=`toast ${type} show`;el.innerHTML=`<i data-lucide="${type==='success'?'circle-check':type==='error'?'circle-alert':'info'}"></i><span>${esc(message)}</span>`;icons();clearTimeout(toast.timer);toast.timer=setTimeout(()=>el.classList.remove('show'),2800)}
function loading(text='正在读取数据...'){return `<div class="page-state loading"><span class="spinner"></span><strong>${text}</strong></div>`}
function empty(icon,title,text,action=''){return `<div class="page-state empty"><span class="state-icon"><i data-lucide="${icon}"></i></span><strong>${title}</strong><p>${text}</p>${action}</div>`}
function errorView(message){return `<div class="page-state error"><span class="state-icon"><i data-lucide="circle-alert"></i></span><strong>页面暂时无法读取</strong><p>${esc(message)}</p><button class="secondary" data-action="reload"><i data-lucide="refresh-cw"></i>重新加载</button></div>`}
function pageHead(label,title,text,action=''){return `<header class="page-head"><div><span class="eyebrow">${label}</span><h1>${title}</h1><p>${text}</p></div>${action?`<div class="page-head-actions">${action}</div>`:''}</header>`}
function metric(icon,label,value,note){return `<article class="metric"><span class="metric-icon"><i data-lucide="${icon}"></i></span><div class="metric-body"><span>${label}</span><strong>${value}</strong><small>${note}</small></div></article>`}
function refreshIcons(){requestAnimationFrame(icons)}

function setButton(button,busy,label){if(!button)return;button.disabled=busy;if(!button.dataset.label)button.dataset.label=button.innerHTML;button.innerHTML=busy?`<span class="button-spinner"></span>${label}`:button.dataset.label;refreshIcons()}
function setMailSetupVisible(visible){const setup=$('#mail-setup');setup.classList.toggle('hidden',!visible);setup.querySelectorAll('input').forEach(input=>{input.disabled=!visible})}
function logout(){localStorage.removeItem('cc_token');state.token=null;state.user=null;go('login')}
function setAuth(mode){
    $('#auth-view').classList.remove('hidden');
    $('#auth-view').dataset.authMode=mode;
    $('#main-view').classList.add('hidden');
    $('#login-form').classList.toggle('hidden',mode!=='login');
    $('#register-form').classList.toggle('hidden',mode!=='register');
    $('#forgot-form').classList.toggle('hidden',mode!=='forgot-password');
    $$('.auth-tabs a').forEach(a=>{
        const active=a.hash===`#/${mode}`;
        a.classList.toggle('active',active);
        if(active)a.setAttribute('aria-current','page');else a.removeAttribute('aria-current');
    });
    const copy={
        login:['登录','继续整理你的错题','查看错题记录、AI 解析结果与复习进度。'],
        register:['注册','创建个人学习账户','集中保存调试代码、报错现象与解析结论。'],
        'forgot-password':['重置','找回账户访问权限','验证注册邮箱验证码后设置新的访问密码。']
    }[mode];
    $('#auth-context-index').textContent=copy[0];
    $('#auth-context-title').textContent=copy[1];
    $('#auth-context-note').textContent=copy[2];
    $('#auth-message').textContent=state.authNotice;
    state.authNotice='';
    const titles={login:'登录',register:'注册','forgot-password':'找回密码'};
    $('#auth-card').setAttribute('aria-label',`账户${titles[mode]}`);
    document.title=`${titles[mode]} · CodeClinic`;
    refreshIcons();
}

async function login(form){const button=form.querySelector('button[type=submit]');setButton(button,true,'正在登录');try{const data=await api('/user/login',{method:'POST',body:JSON.stringify(Object.fromEntries(new FormData(form)))});state.token=data.token;state.user=data.user;localStorage.setItem('cc_token',data.token);go('overview')}catch(e){$('#auth-message').textContent=e.message}finally{setButton(button,false)}}
async function register(form){const button=form.querySelector('button[type=submit]');setButton(button,true,'正在创建');try{await api('/user/register',{method:'POST',body:JSON.stringify(Object.fromEntries(new FormData(form)))});state.authNotice='注册成功，请登录';go('login');toast('账户已创建','success')}catch(e){$('#auth-message').textContent=e.message}finally{setButton(button,false)}}
async function requestResetCode(){const email=$('#reset-email');if(!email.reportValidity())return;const button=$('#send-reset-code');setButton(button,true,'正在发送');$('#auth-message').textContent='';try{const data=await api('/user/password/reset-code',{method:'POST',body:JSON.stringify({email:email.value})});setMailSetupVisible(false);$('#reset-fields').classList.remove('hidden');const note=$('#reset-code-note');note.innerHTML=`<i data-lucide="mail-check"></i><span>若该邮箱已注册且发件服务正常，验证码将发送至 ${esc(data?.maskedEmail||'你的邮箱')}，10 分钟内有效。</span>`;button.dataset.label='<i data-lucide="refresh-cw"></i>重新发送验证码';setButton(button,false);$('#forgot-form [name=code]').focus();refreshIcons()}catch(e){if(e.message.includes('邮件服务未配置')){setMailSetupVisible(true);$('#mail-sender-email').value||=email.value;$('#auth-message').textContent='请先完成下方本机发件邮箱配置'}else{$('#auth-message').textContent=e.message}setButton(button,false);refreshIcons()}}
async function configureResetMail(){const email=$('#mail-sender-email'),authorizationCode=$('#mail-authorization-code'),setupToken=$('#mail-setup-token');if(!email.reportValidity()||!authorizationCode.reportValidity()||!setupToken.reportValidity())return;const button=$('#save-mail-settings');setButton(button,true,'正在验证');$('#auth-message').textContent='';try{await api('/user/password/mail-settings',{method:'POST',body:JSON.stringify({email:email.value,authorizationCode:authorizationCode.value,setupToken:setupToken.value})});authorizationCode.value='';setupToken.value='';toast('发件邮箱配置成功','success');await requestResetCode()}catch(e){$('#auth-message').textContent=e.message}finally{setButton(button,false)}}
async function resetPassword(form){const button=form.querySelector('button[type=submit]');setButton(button,true,'正在重置');$('#auth-message').textContent='';const body={email:$('#reset-email').value,code:form.elements.code.value,newPassword:form.elements.newPassword.value};try{await api('/user/password/reset',{method:'POST',body:JSON.stringify(body)});state.authNotice='密码已更新，请使用新密码登录';form.reset();$('#reset-fields').classList.add('hidden');go('login');toast('密码重置成功','success')}catch(e){$('#auth-message').textContent=e.message;setButton(button,false)}}
function togglePassword(button){const input=button.parentElement.querySelector('input');const showing=input.type==='text';input.type=showing?'password':'text';button.innerHTML=`<i data-lucide="${showing?'eye':'eye-off'}"></i>`;button.setAttribute('aria-label',showing?'显示密码':'隐藏密码');button.title=showing?'显示密码':'隐藏密码';refreshIcons()}
function setSidebar(open){document.body.classList.toggle('sidebar-open',open);$('#sidebar-toggle').setAttribute('aria-expanded',String(open));$('#sidebar-toggle').setAttribute('aria-label',open?'关闭导航':'打开导航')}

async function render(){
    setSidebar(false);
    const r=route();
    if(['login','register','forgot-password'].includes(r.view)){setAuth(r.view);return}
    if(!state.token){go('login');return}
    $('#auth-view').classList.add('hidden');
    $('#main-view').classList.remove('hidden');
    if(!state.user){
        try{state.user=await api('/user/info')}
        catch{localStorage.removeItem('cc_token');state.token=null;go('login');return}
    }
    const name=state.user.displayName||state.user.username;
    $('#user-name').textContent=name;
    $('#user-avatar').textContent=name.slice(0,1).toUpperCase();
    $('#page-name').textContent=pageNames[r.view];
    document.title=`${pageNames[r.view]} · CodeClinic`;
    const navKey=r.view.startsWith('question')||['analysis','history'].includes(r.view)?'questions':r.view;
    $$('[data-nav]').forEach(a=>{
        const active=a.dataset.nav===navKey;
        a.classList.toggle('active',active);
        if(active)a.setAttribute('aria-current','page');else a.removeAttribute('aria-current');
    });
    const content=$('#content');
    content.innerHTML=loading();
    refreshIcons();
    try{
        if(r.view==='overview')await renderOverview();
        if(r.view==='questions')await renderQuestions();
        if(r.view==='questionForm')await renderQuestionForm(r.id);
        if(r.view==='questionDetail')await renderQuestionDetail(r.id);
        if(r.view==='analysis')await renderAnalysis(r.id);
        if(r.view==='history')await renderHistory(r.id);
        if(r.view==='knowledge')await renderKnowledge();
        if(r.view==='stats')await renderStats();
        if(r.view==='reviews')await renderReviews();
    }catch(e){content.innerHTML=errorView(e.message)}
    refreshIcons();
    window.scrollTo(0,0);
}

async function renderOverview(){
    const [s,list,knowledge]=await Promise.all([api('/statistics'),api('/wrong-question?page=1&size=5'),api('/knowledge-point')]);
    const name=esc(state.user.displayName||state.user.username);
    const rate=Math.min(100,Number(s.completionRate)||0);
    const recent=list.records||[];
    $('#content').innerHTML=`
        ${pageHead('WORKSPACE OVERVIEW',`你好，${name}`,'集中查看错题归档、AI 诊断完成率与近期复盘队列。','<a class="primary" href="#/questions/new"><i data-lucide="plus"></i>记录新错题</a>')}
        <section class="metrics-grid">
            ${metric('folder-code','错题总数',s.totalQuestions,'累计归档错题')}
            ${metric('circle-check','已完成解析',s.analyzedQuestions,`完成率 ${s.completionRate}%`)}
            ${metric('clock-3','待解析错题',s.pendingQuestions,'等待 AI 诊断')}
            ${metric('sparkles','累计解析次数',s.totalAnalyses,'历史模型调用')}
        </section>
        <div class="overview-grid">
            <section class="panel">
                <div class="panel-head">
                    <div>
                        <span class="eyebrow">RECENT ARCHIVES</span>
                        <h2>最近录入的错题</h2>
                    </div>
                    <a class="text-link" href="#/questions">查看全部 <i data-lucide="arrow-right"></i></a>
                </div>
                ${recent.length?`<div class="question-list">${recent.map(questionRow).join('')}</div>`:empty('notebook-tabs','尚无错题记录','录入第一道编程错题，建立你的个人代码诊断档案。','<a class="primary" href="#/questions/new"><i data-lucide="plus"></i>记录第一道错题</a>')}
            </section>
            <aside class="overview-side">
                <section class="panel progress-panel">
                    <div class="panel-head">
                        <div>
                            <span class="eyebrow">DIAGNOSIS PROGRESS</span>
                            <h2>解析完成进度</h2>
                        </div>
                    </div>
                    <div class="progress-main">
                        <div>
                            <strong>${rate}%</strong>
                            <span>已解析 ${s.analyzedQuestions} / 共 ${s.totalQuestions} 题</span>
                        </div>
                        <div class="progress-ring" style="--rate:${rate*3.6}deg">
                            <span>${s.analyzedQuestions}/${s.totalQuestions}</span>
                        </div>
                    </div>
                    <div class="progress-track"><i style="width:${rate}%"></i></div>
                    <p class="progress-note">${s.pendingQuestions?`当前仍有 ${s.pendingQuestions} 道错题尚未完成解析，建议优先诊断。`:'所有已录入错题均已完成 AI 解析。'}</p>
                </section>
                <section class="panel next-panel">
                    <div class="panel-head">
                        <div>
                            <span class="eyebrow">RECOMMENDED ACTIONS</span>
                            <h2>学习推进建议</h2>
                        </div>
                    </div>
                    <a class="action-item" href="${s.pendingQuestions?'#/questions':'#/questions/new'}">
                        <span class="step-number">01</span>
                        <div>
                            <strong>${s.pendingQuestions?'处理待解析错题':'录入新的调试案例'}</strong>
                            <small>${s.pendingQuestions?'前往错题列表运行 AI 根因诊断':'保存报错堆栈与输入输出对比'}</small>
                        </div>
                        <i data-lucide="chevron-right"></i>
                    </a>
                    <a class="action-item" href="#/reviews">
                        <span class="step-number">02</span>
                        <div>
                            <strong>执行间隔复习计划</strong>
                            <small>按 1/3/7/14/30 天周期巩固薄弱环节</small>
                        </div>
                        <i data-lucide="chevron-right"></i>
                    </a>
                    <a class="action-item" href="#/knowledge">
                        <span class="step-number">03</span>
                        <div>
                            <strong>浏览知识点地图</strong>
                            <small>已沉淀 ${knowledge.length} 个关联编程知识点</small>
                        </div>
                        <i data-lucide="chevron-right"></i>
                    </a>
                </section>
            </aside>
        </div>`;
}

function questionRow(q){
    const [label,tone,icon]=statusMeta(q.status);
    return `<a class="question-row" href="#/questions/${q.id}">
        <span class="language-badge">${esc((q.language||'JA').slice(0,4))}</span>
        <span class="question-main">
            <strong>${esc(q.title)}</strong>
            <small>${esc(q.language)} <i></i> 更新于 ${fmtDate(q.updateTime||q.createTime)}</small>
        </span>
        <span class="status ${tone}"><i data-lucide="${icon}"></i>${label}</span>
        <i class="row-chevron" data-lucide="chevron-right"></i>
    </a>`;
}

async function renderQuestions(){
    const query=new URLSearchParams({page:String(state.listPage),size:'10'});
    Object.entries(state.filters).forEach(([k,v])=>{if(v!=='')query.set(k,v)});
    const list=await api(`/wrong-question?${query}`);
    const hasFilter=Object.values(state.filters).some(v=>v!=='');
    $('#content').innerHTML=`
        ${pageHead('QUESTION ARCHIVE','我的错题库','按关键词、编程语言、错误类型与解析状态检索管理错题档案。','<a class="primary" href="#/questions/new"><i data-lucide="plus"></i>记录新错题</a>')}
        <section class="panel list-panel">
            <form id="filter-form" class="filter-bar">
                <label class="search-input">
                    <span>关键词检索</span>
                    <div class="input-with-icon">
                        <i data-lucide="search"></i>
                        <input name="keyword" aria-label="搜索错题" value="${attr(state.filters.keyword)}" placeholder="搜索题目标题、描述或代码片段">
                    </div>
                </label>
                <label>
                    <span>编程语言</span>
                    <select name="language">
                        <option value="">全部语言</option>
                        ${['Java','Python','JavaScript','C++','Go'].map(v=>`<option ${state.filters.language===v?'selected':''}>${v}</option>`).join('')}
                    </select>
                </label>
                <label>
                    <span>错误类型</span>
                    <input name="errorType" maxlength="100" value="${attr(state.filters.errorType||'')}" placeholder="如：数组越界 / 空指针">
                </label>
                <label>
                    <span>解析状态</span>
                    <select name="status">
                        <option value="">全部状态</option>
                        <option value="0" ${state.filters.status==='0'?'selected':''}>待分析</option>
                        <option value="1" ${state.filters.status==='1'?'selected':''}>分析中</option>
                        <option value="2" ${state.filters.status==='2'?'selected':''}>已分析</option>
                        <option value="3" ${state.filters.status==='3'?'selected':''}>分析失败</option>
                    </select>
                </label>
                <div class="filter-actions">
                    <button class="primary" type="submit"><i data-lucide="sliders-horizontal"></i>筛选</button>
                    ${hasFilter?'<button class="secondary" type="button" data-action="clear-filters">重置</button>':''}
                </div>
            </form>
            <div class="list-summary">
                <span>共检索到 <strong>${list.total}</strong> 条错题记录</span>
                <span>第 ${list.current||1} / ${Math.max(1,list.pages||1)} 页</span>
            </div>
            ${list.records.length?`<div class="question-list">${list.records.map(questionRow).join('')}</div>${pagination(list)}`:empty('search-x','未找到匹配的错题','请尝试放宽筛选条件，或直接录入一道新的编程错题。','<button class="secondary" data-action="clear-filters">清除筛选条件</button>')}
        </section>`;
}

function pagination(list){
    const current=Number(list.current)||1,pages=Math.max(1,Number(list.pages)||1);
    if(pages<=1)return'';
    return `<nav class="pagination" aria-label="分页导航">
        <button data-page="${current-1}" ${current<=1?'disabled':''} aria-label="上一页"><i data-lucide="chevron-left"></i></button>
        ${Array.from({length:pages},(_,i)=>i+1).filter(p=>p===1||p===pages||Math.abs(p-current)<=1).map((p,i,a)=>`${i&&p-a[i-1]>1?'<span class="page-ellipsis">…</span>':''}<button data-page="${p}" class="${p===current?'active':''}">${p}</button>`).join('')}
        <button data-page="${current+1}" ${current>=pages?'disabled':''} aria-label="下一页"><i data-lucide="chevron-right"></i></button>
    </nav>`;
}

function detailTabs(id,active){
    return `<nav class="detail-tabs" aria-label="错题详情切换">
        <a class="${active==='detail'?'active':''}" ${active==='detail'?'aria-current="page"':''} href="#/questions/${id}"><i data-lucide="file-code-2"></i>问题详情</a>
        <a class="${active==='analysis'?'active':''}" ${active==='analysis'?'aria-current="page"':''} href="#/questions/${id}/analysis"><i data-lucide="sparkles"></i>AI 智能解析</a>
        <a class="${active==='history'?'active':''}" ${active==='history'?'aria-current="page"':''} href="#/questions/${id}/history"><i data-lucide="history"></i>解析历史</a>
    </nav>`;
}

async function renderQuestionForm(id){
    let q={language:'Java'};
    if(id){
        const d=await api(`/wrong-question/${id}`);
        q=d.question;
        if(Number(q.status)===1)throw new Error('该错题正在分析中，请等待分析完成后再编辑');
    }
    $('#content').innerHTML=`
        <a class="back-link" href="${id?`#/questions/${id}`:'#/questions'}"><i data-lucide="arrow-left"></i>返回${id?'错题详情':'我的错题库'}</a>
        ${pageHead(id?'EDIT ARCHIVE':'NEW ARCHIVE',id?'编辑错题档案':'录入编程错题',id?'修改题目或代码后，建议重新执行 AI 解析以同步最新诊断结论。':'完整填写题目要求、错误代码、报错信息及实际/预期输出，有助于提高大模型根因定位准确度。')}
        <form id="question-form" class="question-form" data-id="${id||''}">
            <section class="panel form-section">
                <div class="section-heading">
                    <span class="step-number">01</span>
                    <div>
                        <h2>题目基本信息</h2>
                        <p>定义错题标题、所属编程语言与题目背景描述。</p>
                    </div>
                </div>
                <div class="form-grid">
                    <label>题目标题 <span class="required">*</span>
                        <input name="title" value="${attr(q.title||'')}" required maxlength="200" placeholder="例如：二分查找边界条件导致死循环">
                    </label>
                    <label>编程语言 <span class="required">*</span>
                        <input name="language" value="${attr(q.language||'Java')}" required maxlength="50" list="language-options">
                        <datalist id="language-options"><option>Java</option><option>Python</option><option>JavaScript</option><option>C++</option><option>Go</option></datalist>
                    </label>
                    <label class="full">题目描述与要求
                        <textarea name="description" maxlength="5000" placeholder="填写题目背景、输入输出格式或约束条件">${esc(q.description||'')}</textarea>
                    </label>
                </div>
            </section>
            <section class="panel form-section">
                <div class="section-heading">
                    <span class="step-number">02</span>
                    <div>
                        <h2>错误代码与运行现象</h2>
                        <p>保留出错时的完整源代码、编译器/运行时报错以及输出差异。</p>
                    </div>
                </div>
                <label>出错源代码 <span class="required">*</span>
                    <textarea class="code-input" name="code" required maxlength="30000" spellcheck="false" placeholder="粘贴包含错误的完整代码片段">${esc(q.code||'')}</textarea>
                </label>
                <div class="form-grid">
                    <label>报错信息 / 异常堆栈
                        <textarea name="errorMessage" maxlength="10000" placeholder="粘贴编译报错或运行时异常堆栈信息">${esc(q.errorMessage||'')}</textarea>
                    </label>
                    <label>个人排查备注
                        <textarea name="userNote" maxlength="3000" placeholder="记录你当前的排查思路或疑惑点">${esc(q.userNote||'')}</textarea>
                    </label>
                    <label>程序实际输出
                        <textarea name="actualOutput" maxlength="10000" placeholder="程序当前错误运行产生的输出">${esc(q.actualOutput||'')}</textarea>
                    </label>
                    <label>题目预期输出
                        <textarea name="expectedOutput" maxlength="10000" placeholder="标准答案或期望得到的正确输出">${esc(q.expectedOutput||'')}</textarea>
                    </label>
                </div>
            </section>
            <div class="form-actions">
                <a class="secondary" href="${id?`#/questions/${id}`:'#/questions'}">取消</a>
                <button class="primary" type="submit"><i data-lucide="save"></i>${id?'保存修改':'保存错题档案'}</button>
            </div>
        </form>`;
}

async function saveQuestion(form){
    const id=form.dataset.id;
    const data=Object.fromEntries(new FormData(form));
    const button=form.querySelector('button[type=submit]');
    setButton(button,true,'正在保存');
    try{
        const result=await api(id?`/wrong-question/${id}`:'/wrong-question',{method:id?'PUT':'POST',body:JSON.stringify(data)});
        toast(id?'错题已更新':'错题已保存','success');
        go(`questions/${id||result}`);
    }catch(e){
        toast(e.message,'error');
        setButton(button,false);
    }
}

async function renderQuestionDetail(id){
    const d=await api(`/wrong-question/${id}`),q=d.question,a=d.analysis,[label,tone,icon]=statusMeta(q.status);
    $('#content').innerHTML=`
        <a class="back-link" href="#/questions"><i data-lucide="arrow-left"></i>返回我的错题库</a>
        <header class="detail-head">
            <div>
                <div class="detail-meta">
                    <span class="language-pill">${esc(q.language)}</span>
                    <span class="status ${tone}"><i data-lucide="${icon}"></i>${label}</span>
                    <span class="meta-date">记录于 ${fmtDate(q.createTime)}</span>
                </div>
                <h1>${esc(q.title)}</h1>
                <p>${esc(q.description||'未填写题目背景描述')}</p>
            </div>
            <div class="detail-actions">
                ${Number(q.status)===1?'<span class="status running">分析进行中，暂不可编辑</span>':`
                    <a class="secondary" href="#/questions/${id}/edit"><i data-lucide="pencil"></i>编辑错题</a>
                    <button class="secondary danger-outline" data-action="delete" data-id="${id}" title="删除错题"><i data-lucide="trash-2"></i>删除</button>
                `}
            </div>
        </header>
        ${detailTabs(id,'detail')}
        <div class="detail-layout">
            <section class="detail-main">
                <article class="panel code-panel">
                    <div class="panel-head">
                        <div>
                            <span class="eyebrow">SOURCE CODE</span>
                            <h2>出错源代码</h2>
                        </div>
                        <button class="secondary compact-btn" data-action="copy" data-id="${id}" title="复制代码"><i data-lucide="copy"></i>复制代码</button>
                    </div>
                    <pre><code>${esc(q.code)}</code></pre>
                </article>
                <article class="panel info-panel">
                    <div class="panel-head">
                        <div>
                            <span class="eyebrow">EXECUTION CONTEXT</span>
                            <h2>运行现象与输出对比</h2>
                        </div>
                    </div>
                    <dl class="info-grid">
                        <div><dt>报错信息</dt><dd>${esc(q.errorMessage||'未提供报错信息')}</dd></div>
                        <div><dt>个人备注</dt><dd>${esc(q.userNote||'未添加个人备注')}</dd></div>
                        <div><dt>实际输出</dt><dd>${esc(q.actualOutput||'未提供实际输出')}</dd></div>
                        <div><dt>预期输出</dt><dd>${esc(q.expectedOutput||'未提供预期输出')}</dd></div>
                    </dl>
                </article>
            </section>
            <aside class="panel analysis-preview">
                <span class="preview-icon"><i data-lucide="sparkles"></i></span>
                <span class="eyebrow">AI DIAGNOSIS</span>
                <h2>${Number(q.status)===1?'正在生成解析':Number(q.status)===3?'上次解析未完成':a?'AI 解析已就绪':'等待执行 AI 解析'}</h2>
                <p>${Number(q.status)===1?(a?'新的解析任务正在后台运行，当前可先查看上一版解析结果。':'模型正在定位错误原因与修正方案，请稍候刷新查看。'):Number(q.status)===3?(a?'最近一次解析请求未成功完成，你可以查看历史解析或重新发起。':'最近一次解析请求未成功，请确认本地 Ollama 或模型接口可用后重试。'):a?`已识别错误类型为「${esc(a.errorType||'程序错误')}」，包含根因说明、修改建议与参考代码。`:'系统将结合代码、报错堆栈与输出差异进行结构化诊断，并提取关联知识点。'}</p>
                ${Number(q.status)===1?(a?`<a class="primary wide" href="#/questions/${id}/analysis">查看已有解析 <i data-lucide="arrow-right"></i></a>`:'<button class="primary wide" disabled>正在分析中...</button>'):a?`<a class="primary wide" href="#/questions/${id}/analysis">查看完整解析 <i data-lucide="arrow-right"></i></a>`:`<button class="primary wide" data-action="analyze" data-id="${id}"><i data-lucide="sparkles"></i>开始 AI 智能解析</button>`}
                <a class="history-link" href="#/questions/${id}/history"><i data-lucide="history"></i>查看历次解析记录</a>
            </aside>
        </div>`;
}

async function renderAnalysis(id){
    const [d,service]=await Promise.all([
        api(`/wrong-question/${id}`),
        api('/analysis/status').catch(()=>({available:false,model:'AI',message:'暂时无法读取模型状态'}))
    ]);
    const q=d.question,a=d.analysis;
    const feedback=a?await api(`/analysis/${a.id}/feedback`).catch(()=>({})):null;
    const stateClass=service.available?'ready':'unavailable';
    $('#content').innerHTML=`
        <a class="back-link" href="#/questions/${id}"><i data-lucide="arrow-left"></i>返回错题详情</a>
        <header class="subpage-head">
            <div>
                <span class="eyebrow">${esc(q.language)} · ${esc(q.title)}</span>
                <h1>AI 智能解析报告</h1>
                <p>${Number(q.status)===1?'正在后台生成新一轮解析。':Number(q.status)===3?'最近一次解析未完成。':''}${a?`由模型 ${esc(a.modelName||'AI')} 生成于 ${fmtTime(a.createTime)}${Number(q.status)===2?'':'（历史保留版本）'}。`:'当前错题尚未生成解析报告。'}</p>
                <div class="ai-service-state ${stateClass}" role="status">
                    <i data-lucide="${service.available?'circle-check':'circle-alert'}"></i>
                    <span>${esc(service.message)} · 当前模型：${esc(service.model||'未配置')}</span>
                </div>
            </div>
            <button class="secondary" data-action="analyze" data-id="${id}" ${Number(q.status)===1?'disabled':''}>
                <i data-lucide="refresh-cw"></i>${Number(q.status)===1?'正在分析':a?'重新生成解析':'开始解析'}
            </button>
        </header>
        ${detailTabs(id,'analysis')}
        ${a?analysisContent(a,feedback):Number(q.status)===1?empty('sparkles','AI 正在分析错题','请稍候刷新页面查看结构化诊断结果。',''):empty('sparkles','尚未生成 AI 解析',service.available?'点击下方按钮，系统将自动定位错误位置、分析原因并给出参考修正代码。':'当前 AI 推理服务不可用，请确认 Ollama 或模型配置已启动。',`<button class="primary" data-action="analyze" data-id="${id}"><i data-lucide="sparkles"></i>检查服务并开始解析</button>`)}`;
}

function feedbackContent(a,f){
    if(!f)return '';
    const verdict=f.verdict||'unreviewed';
    return `<section class="panel feedback-panel">
        <div class="panel-head">
            <div>
                <span class="eyebrow">HUMAN VERIFICATION</span>
                <h2>人工核验与评价</h2>
            </div>
        </div>
        <p class="feedback-hint">系统不在线运行用户代码，请结合本地编译与测试结果核验模型给出的参考代码与原因分析是否准确。</p>
        <form id="feedback-form" data-analysis-id="${a.id}">
            <div class="feedback-grid">
                <label>核验结论
                    <select name="verdict" required>
                        <option value="uncertain" ${verdict==='uncertain'||verdict==='unreviewed'?'selected':''}>暂不确定（待验证）</option>
                        <option value="accurate" ${verdict==='accurate'?'selected':''}>分析准确（定位与代码正确）</option>
                        <option value="inaccurate" ${verdict==='inaccurate'?'selected':''}>分析有误（存在偏差或错误）</option>
                    </select>
                </label>
                <label>核验备注说明
                    <textarea name="comment" maxlength="1000" placeholder="记录本地测试验证结果、采纳情况或模型存在的偏差">${esc(f.comment||'')}</textarea>
                </label>
            </div>
            <div class="feedback-actions">
                <button class="primary" type="submit"><i data-lucide="check"></i>保存核验记录</button>
            </div>
        </form>
    </section>`;
}

function analysisContent(a,feedback=null){
    const points=Array.isArray(a.knowledgePoints)?a.knowledgePoints:[];
    return `<section class="analysis-result">
        <article class="analysis-summary">
            <span class="analysis-symbol"><i data-lucide="scan-search"></i></span>
            <div class="analysis-summary-body">
                <span>诊断错误类型与定位位置</span>
                <h2>${esc(a.errorType||'程序逻辑或语法错误')}</h2>
                <code>定位位置：${esc(a.errorLocation||'未标注具体行号')}</code>
            </div>
        </article>
        <div class="analysis-columns">
            <div class="analysis-col">
                <article class="panel analysis-block">
                    <span class="block-number">01</span>
                    <div>
                        <span class="eyebrow">ROOT CAUSE</span>
                        <h2>错误原因分析</h2>
                        <p>${esc(a.errorReason||'暂无错误原因说明')}</p>
                    </div>
                </article>
                <article class="panel analysis-block">
                    <span class="block-number">02</span>
                    <div>
                        <span class="eyebrow">FIX STRATEGY</span>
                        <h2>修改方向与建议</h2>
                        <p>${esc(a.suggestion||'暂无修改建议')}</p>
                    </div>
                </article>
                <article class="panel analysis-block">
                    <span class="block-number">03</span>
                    <div>
                        <span class="eyebrow">KNOWLEDGE &amp; ADVICE</span>
                        <h2>关联知识点与学习建议</h2>
                        <div class="chips">${points.length?points.map(p=>`<span>${esc(p)}</span>`).join(''):'<span>未提取知识点</span>'}</div>
                        <p class="advice-text">${esc(a.learningAdvice||'建议结合同类边界条件进行巩固练习。')}</p>
                    </div>
                </article>
            </div>
            <aside class="analysis-col">
                <article class="panel code-panel result-code">
                    <div class="panel-head">
                        <div>
                            <span class="eyebrow">REFERENCE IMPLEMENTATION</span>
                            <h2>参考修正代码（待离线核验）</h2>
                        </div>
                    </div>
                    <pre><code>${esc(a.correctCode||'暂无参考代码')}</code></pre>
                </article>
                ${feedbackContent(a,feedback)}
            </aside>
        </div>
    </section>`;
}

async function runAnalysis(id,button){
    setButton(button,true,'正在检查服务');
    let timer=null;
    try{
        const service=await api('/analysis/status');
        if(!service.available)throw new Error(`${service.message}（${service.model||'未配置模型'}）`);
        let seconds=0;
        timer=setInterval(()=>{
            seconds+=1;
            if(button)button.innerHTML=`<span class="button-spinner"></span>正在解析 ${seconds}s`;
        },1000);
        toast(`正在调用 ${service.model} 生成结构化解析`);
        await api(`/wrong-question/${id}/reanalyze`,{method:'POST'});
        toast('AI 解析已完成并保存','success');
        const current=route();
        if(current.view==='analysis'&&current.id===Number(id))await render();
        else go(`questions/${id}/analysis`);
    }catch(e){
        toast(e.message,'error');
        setButton(button,false);
    }finally{
        if(timer)clearInterval(timer);
    }
}

async function renderHistory(id){
    const [d,history,failures]=await Promise.all([api(`/wrong-question/${id}`),api(`/analysis/${id}/history`),api(`/analysis/${id}/failures`)]);
    const q=d.question;
    $('#content').innerHTML=`
        <a class="back-link" href="#/questions/${id}"><i data-lucide="arrow-left"></i>返回错题详情</a>
        ${pageHead(`${esc(q.language)} · ${esc(q.title)}`,'解析历史记录','留存每次重新解析的完整结果与当时的原题输入快照，便于对比排查演进过程。')}
        ${detailTabs(id,'history')}
        ${failures.length?`
            <section class="panel failure-panel">
                <div class="panel-head">
                    <div>
                        <span class="eyebrow">INTERRUPTED OR FAILED RUNS</span>
                        <h2>未完成的解析调用记录</h2>
                    </div>
                    <a class="secondary compact-btn" href="#/questions/${id}/analysis"><i data-lucide="refresh-cw"></i>前往重试</a>
                </div>
                <div class="failure-list">
                    ${failures.map(f=>`<div class="failure-item"><span>记录 #${f.id}</span><span>${fmtTime(f.createTime)}</span><span>${esc(f.reason)}</span></div>`).join('')}
                </div>
            </section>
        `:''}
        <section class="panel history-panel">
            ${history.length?`<div class="history-list">${history.map((item,index)=>`
                <article class="history-item">
                    <div class="history-item-head">
                        <span class="step-number">${String(history.length-index).padStart(2,'0')}</span>
                        <div class="history-meta">
                            <strong>第 ${history.length-index} 次解析（记录编号 #${item.id}）</strong>
                            <small>${esc(item.errorType||'程序错误')} · 生成于 ${fmtTime(item.createTime)} · 模型：${esc(item.modelName||'AI')}</small>
                        </div>
                    </div>
                    <details class="history-details">
                        <summary>展开查看本次解析详情与原题快照</summary>
                        <div class="history-details-body">
                            ${historySnapshot(item)}
                            ${analysisContent(item)}
                        </div>
                    </details>
                </article>`).join('')}</div>`:empty('history','暂无历史解析记录','完成第一次 AI 智能解析后，历次解析记录会自动归档在此处。',`<a class="primary" href="#/questions/${id}/analysis">前往执行解析</a>`)}
        </section>`;
}

function historySnapshot(item){
    if(!item.inputSnapshot)return '<div class="snapshot-box"><p class="muted-note">该条早期记录未保存原题输入快照。</p></div>';
    try{
        const q=JSON.parse(item.inputSnapshot);
        return `<details class="snapshot-details">
            <summary>查看解析时的原题输入快照（${esc(q.title)}）</summary>
            <div class="snapshot-box">
                <div class="snapshot-meta"><strong>${esc(q.title)}</strong> <span>(${esc(q.language)})</span></div>
                <p>${esc(q.description||'无题目描述')}</p>
                <pre><code>${esc(q.code||'')}</code></pre>
                <div class="snapshot-io">
                    <div><strong>报错信息：</strong>${esc(q.errorMessage||'未提供')}</div>
                    <div><strong>实际输出：</strong>${esc(q.actualOutput||'未提供')}</div>
                    <div><strong>预期输出：</strong>${esc(q.expectedOutput||'未提供')}</div>
                </div>
            </div>
        </details>`;
    }catch{
        return '<div class="snapshot-box"><p class="muted-note">原题快照格式无法解析</p></div>';
    }
}

async function renderKnowledge(){
    const list=await api('/knowledge-point');
    const groups=groupKnowledge(list);
    $('#content').innerHTML=`
        ${pageHead('KNOWLEDGE GRAPH','知识点地图','由 AI 错题解析自动提取并归类的编程知识点索引，辅助定位知识盲区。')}
        <section class="panel knowledge-hero">
            <div class="knowledge-count">
                <span>${list.length}</span>
                <strong>个已沉淀知识点</strong>
            </div>
            <p>每当错题完成 AI 解析，识别出的核心语法、数据结构或框架概念会自动归入下方分类目录。</p>
        </section>
        ${list.length?`
            <div class="knowledge-layout">
                <aside class="panel knowledge-index">
                    <span class="eyebrow">CATEGORIES</span>
                    <nav>
                        ${Object.entries(groups).map(([name,items],i)=>`<a href="#knowledge-${i}"><span>${name}</span><em>${items.length}</em></a>`).join('')}
                    </nav>
                </aside>
                <div class="knowledge-groups">
                    ${Object.entries(groups).map(([name,items],i)=>`
                        <section id="knowledge-${i}" class="knowledge-section">
                            <header class="knowledge-section-head">
                                <span class="step-number">${String(i+1).padStart(2,'0')}</span>
                                <div>
                                    <h2>${name}</h2>
                                    <p>包含 ${items.length} 个关联知识点</p>
                                </div>
                            </header>
                            <div class="knowledge-grid">
                                ${items.map(k=>`
                                    <article class="panel knowledge-card">
                                        <div class="knowledge-card-top">
                                            <i data-lucide="book-open-check"></i>
                                            <strong>${esc(k.name)}</strong>
                                        </div>
                                        <p>${esc(k.description||'由错题 AI 解析自动归纳沉淀')}</p>
                                        <small>收录于 ${fmtDate(k.createTime)}</small>
                                    </article>
                                `).join('')}
                            </div>
                        </section>
                    `).join('')}
                </div>
            </div>
        `:empty('network','知识点地图尚为空白','对错题执行 AI 解析后，提取出的知识点会自动在此处分类汇聚。','<a class="primary" href="#/questions">前往错题库</a>')}`;
}

function groupKnowledge(list){
    return list.reduce((acc,item)=>{
        const n=(item.name||'').toLowerCase();
        let g='编程语言与语法基础';
        if(/sql|数据库|mybatis|jdbc|事务/.test(n))g='数据库与持久化';
        else if(/spring|接口|api|http|web|jwt|认证/.test(n))g='Web 与服务端开发';
        else if(/算法|数据结构|复杂度|数组|链表|树|查找|排序|递归|边界/.test(n))g='算法与数据结构';
        else if(/异常|调试|错误|日志|空指针|越界|并发/.test(n))g='异常处理与程序健壮性';
        (acc[g]??=[]).push(item);
        return acc;
    },{});
}

async function renderStats(){
    const [s,list]=await Promise.all([api('/statistics'),api('/wrong-question?page=1&size=100')]);
    const counts=list.records.reduce((m,q)=>{m[q.language||'其他']=(m[q.language||'其他']||0)+1;return m},{});
    const max=Math.max(1,...Object.values(counts));
    const rate=Math.min(100,Number(s.completionRate)||0);
    $('#content').innerHTML=`
        ${pageHead('LEARNING ANALYTICS','学习统计','量化统计错题编程语言分布与 AI 诊断覆盖率，掌握整体复盘节奏。')}
        <section class="metrics-grid">
            ${metric('folder-code','错题总数',s.totalQuestions,'累计录入')}
            ${metric('circle-check','已解析错题',s.analyzedQuestions,`占比 ${s.completionRate}%`)}
            ${metric('clock-3','待处理错题',s.pendingQuestions,'等待复盘')}
            ${metric('sparkles','解析总次数',s.totalAnalyses,'累计调用')}
        </section>
        <div class="stats-layout">
            <section class="panel chart-panel">
                <div class="panel-head">
                    <div>
                        <span class="eyebrow">LANGUAGE DISTRIBUTION</span>
                        <h2>错题编程语言分布</h2>
                    </div>
                </div>
                ${Object.keys(counts).length?`<div class="bar-list">${Object.entries(counts).sort((a,b)=>b[1]-a[1]).map(([name,count])=>`
                    <div class="bar-row">
                        <span class="bar-label">${esc(name)}</span>
                        <div class="bar-track"><i style="width:${count/max*100}%"></i></div>
                        <strong class="bar-val">${count}</strong>
                    </div>`).join('')}</div>`:empty('bar-chart-3','暂无语言分布数据','录入错题后，此处将自动展示各编程语言的错题占比。')}
            </section>
            <section class="panel completion-panel">
                <div class="panel-head">
                    <div>
                        <span class="eyebrow">COMPLETION RATIO</span>
                        <h2>解析覆盖率统计</h2>
                    </div>
                </div>
                <div class="donut" style="--rate:${rate*3.6}deg">
                    <span><strong>${rate}%</strong><small>解析完成率</small></span>
                </div>
                <div class="legend">
                    <span><i class="legend-done"></i>已完成解析<strong>${s.analyzedQuestions} 题</strong></span>
                    <span><i class="legend-pending"></i>待解析处理<strong>${s.pendingQuestions} 题</strong></span>
                </div>
            </section>
        </div>`;
}

async function renderReviews(){
    const [list,summary]=await Promise.all([api(`/review/page?page=${state.reviewPage}&size=20`),api('/review/summary')]);
    const all=list.records||[];
    const ready=new Set(all.filter(item=>Number(item.due)===1).map(item=>Number(item.questionId)));
    $('#content').innerHTML=`
        ${pageHead('EBBINGHAUS REVIEW','艾宾浩斯复习计划','根据每次回顾的掌握情况，系统按 1、3、7、14、30 天间隔动态计算下次复习时间。')}
        <section class="metrics-grid">
            ${metric('calendar-clock','当前待复习',Number(summary.due)||0,'已到复习时间')}
            ${metric('folder-code','纳入复习总数',Number(summary.total)||0,`第 ${list.current||1}/${Math.max(1,list.pages||1)} 页`)}
            ${metric('badge-check','已熟练掌握',Number(summary.mastered)||0,'连续记住 5 轮')}
            ${metric('rotate-ccw','周期巩固中',Number(summary.reviewing)||0,'正在按周期推进')}
        </section>
        <section class="panel">
            <div class="panel-head">
                <div>
                    <span class="eyebrow">REVIEW QUEUE</span>
                    <h2>错题复习打卡队列</h2>
                </div>
            </div>
            ${all.length?`<div class="review-list">${all.map(item=>{
                const isDue=ready.has(Number(item.questionId));
                const stateLabel=item.state==='mastered'?'已掌握':item.state==='reviewing'?'巩固中':'待复习';
                return `<article class="review-row">
                    <div class="review-main">
                        <div class="review-title-row">
                            <span class="language-badge">${esc((item.language||'JA').slice(0,4))}</span>
                            <a class="review-title" href="#/questions/${item.questionId}"><strong>${esc(item.title)}</strong></a>
                            <span class="status ${isDue?'analyzed':'pending'}">${isDue?'今日待复习':stateLabel}</span>
                        </div>
                        <small class="review-meta">编程语言：${esc(item.language)} · 已复习 ${item.reviewCount} 次 · 下次计划：${item.nextReviewAt?fmtDate(item.nextReviewAt):'立即复习'}</small>
                    </div>
                    <div class="review-actions">
                        <input type="text" class="review-note-input" data-review-note="${item.questionId}" maxlength="500" aria-label="复习备注 ${item.questionId}" placeholder="填写本次复习备注（选填）">
                        <button class="secondary compact-btn" data-action="review" data-id="${item.questionId}" data-remembered="false" ${isDue?'':'disabled'}>还需练习</button>
                        <button class="primary compact-btn" data-action="review" data-id="${item.questionId}" data-remembered="true" ${isDue?'':'disabled'}>已经记住</button>
                        <button class="secondary compact-btn" data-action="review-history" data-id="${item.questionId}" data-history-page="1">复习记录</button>
                    </div>
                    <div id="review-history-${item.questionId}" class="review-history-slot"></div>
                </article>`;
            }).join('')}</div>`:empty('calendar-check','暂无复习队列','录入编程错题后，系统会自动将其纳入艾宾浩斯复习计划。','<a class="primary" href="#/questions/new">记录新错题</a>')}
            ${pagination(list)}
        </section>`;
}

async function saveReview(id,remembered,button){
    setButton(button,true,'保存中');
    try{
        await api(`/review/${id}`,{method:'POST',body:JSON.stringify({remembered,note:$(`[data-review-note="${id}"]`)?.value||''})});
        toast('复习打卡记录已更新','success');
        renderReviews();
    }catch(e){
        toast(e.message,'error');
        setButton(button,false);
    }
}

async function saveFeedback(form){
    const id=form.dataset.analysisId;
    const button=form.querySelector('button[type=submit]');
    setButton(button,true,'保存中');
    try{
        const data=Object.fromEntries(new FormData(form));
        await api(`/analysis/${id}/feedback`,{method:'PUT',body:JSON.stringify(data)});
        toast('人工核验记录已保存','success');
    }catch(e){
        toast(e.message,'error');
    }finally{
        setButton(button,false);
    }
}

async function copyCode(id){
    try{
        const d=await api(`/wrong-question/${id}`);
        await navigator.clipboard.writeText(d.question.code||'');
        toast('源代码已复制到剪贴板','success');
    }catch(e){
        toast('复制失败','error');
    }
}

async function confirmDelete(id){
    const dialog=$('#delete-dialog');
    dialog.showModal();
    refreshIcons();
    dialog.addEventListener('close',async function done(){
        dialog.removeEventListener('close',done);
        if(dialog.returnValue!=='confirm')return;
        try{
            await api(`/wrong-question/${id}`,{method:'DELETE'});
            toast('错题已删除','success');
            go('questions');
        }catch(e){
            toast(e.message,'error');
        }
    },{once:true});
}

async function showReviewHistory(id,page){
    try{
        const rows=await api(`/review/${id}/history?page=${page}&size=20`);
        $(`#review-history-${id}`).innerHTML=`
            <section class="review-history-box">
                <div class="review-history-head">
                    <strong>打卡历史流水（第 ${page} 页）</strong>
                    <div class="review-history-pager">
                        <button class="secondary compact-btn" data-action="review-history" data-id="${id}" data-history-page="${page-1}" ${page<=1?'disabled':''}>上一页</button>
                        <button class="secondary compact-btn" data-action="review-history" data-id="${id}" data-history-page="${page+1}" ${rows.length<20?'disabled':''}>下一页</button>
                    </div>
                </div>
                ${rows.length?`<div class="review-history-items">${rows.map(x=>`
                    <div class="review-history-item">
                        <span class="status ${Number(x.remembered)?'analyzed':'pending'}">${Number(x.remembered)?'已经记住':'还需练习'}</span>
                        <span>${fmtTime(x.createTime)}</span>
                        <span class="review-history-note">${esc(x.note||'未填写备注')}</span>
                    </div>`).join('')}</div>`:'<p class="muted-note">当前页暂无打卡记录</p>'}
            </section>`;
    }catch(e){
        toast(e.message,'error');
    }
}

$('#login-form').addEventListener('submit',e=>{e.preventDefault();login(e.currentTarget)});
$('#register-form').addEventListener('submit',e=>{e.preventDefault();register(e.currentTarget)});
$('#forgot-form').addEventListener('submit',e=>{e.preventDefault();resetPassword(e.currentTarget)});
$('#send-reset-code').addEventListener('click',requestResetCode);
$('#save-mail-settings').addEventListener('click',configureResetMail);
$('#open-mail-setup').addEventListener('click',()=>{setMailSetupVisible(true);$('#mail-sender-email').value||=$('#reset-email').value;$('#mail-sender-email').focus()});
$('#auth-view').addEventListener('click',e=>{const button=e.target.closest('.password-toggle');if(button)togglePassword(button)});
$('#sidebar-toggle').addEventListener('click',()=>setSidebar(!document.body.classList.contains('sidebar-open')));
$('#sidebar-scrim').addEventListener('click',()=>setSidebar(false));
document.addEventListener('keydown',e=>{if(e.key==='Escape')setSidebar(false)});
$('#logout-btn').addEventListener('click',logout);
$$('.theme-toggle').forEach(button=>button.addEventListener('click',toggleTheme));
$('#content').addEventListener('submit',e=>{
    if(e.target.id==='filter-form'){
        e.preventDefault();
        const data=Object.fromEntries(new FormData(e.target));
        state.filters={keyword:data.keyword.trim(),language:data.language,status:data.status,errorType:data.errorType.trim()};
        state.listPage=1;
        render();
    }
    if(e.target.id==='question-form'){e.preventDefault();saveQuestion(e.target)}
    if(e.target.id==='feedback-form'){e.preventDefault();saveFeedback(e.target)}
});
$('#content').addEventListener('click',e=>{
    const button=e.target.closest('button');
    if(!button)return;
    if(button.dataset.action==='reload')render();
    if(button.dataset.action==='clear-filters'){state.filters={keyword:'',language:'',status:'',errorType:''};state.listPage=1;render()}
    if(button.dataset.page){if(route().view==='reviews')state.reviewPage=Number(button.dataset.page);else state.listPage=Number(button.dataset.page);render()}
    if(button.dataset.action==='copy')copyCode(button.dataset.id);
    if(button.dataset.action==='delete')confirmDelete(button.dataset.id);
    if(button.dataset.action==='analyze')runAnalysis(button.dataset.id,button);
    if(button.dataset.action==='review-history')showReviewHistory(button.dataset.id,Number(button.dataset.historyPage||1));
    if(button.dataset.action==='review')saveReview(button.dataset.id,button.dataset.remembered==='true',button);
});
window.addEventListener('hashchange',render);
syncThemeControls();
render();
