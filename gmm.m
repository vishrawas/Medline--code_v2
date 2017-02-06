
clear all
clc
K = 300;
dimension=300;
ipPath = '/Users/super-machine/Documents/Research/medline/output/EmbeddingFiles/';
opPath = '/Users/super-machine/Documents/Research/medline/output/cluster/';
[pathstr1,name,ext]=fileparts(ipPath);
files = dir(ipPath);
for file = files'   
[pathstr,name,ext]=fileparts(file.name);
if isequal(ext,'.')==false
   
   file.name
   tic
[data,~,~]=importdata(fullfile(pathstr1,file.name));
data = data.data;
mn = min(data); 
mx = max(data);
D = size(data,2);    % data dimension
cInd = kmeans(data, K, 'EmptyAction','singleton');
% fit a GMM model
gmm = fitgmdist(data, K, 'Options',statset('MaxIter',1000), ...
    'CovType','full', 'SharedCov',false, 'Regularize',0.01, 'Start',cInd);
mu = gmm.mu;
sigma = gmm.Sigma;
p = gmm.PComponents;
[pathstr,name,ext] = fileparts(file.name);
f = fullfile(opPath,name);
 
 
% cluster and posterior probablity of each instance
% note that: [~,clustIdx] = max(p,[],2)
[clustInd,~,p] = cluster(gmm, data);
csvwrite(f,p);
f = fullfile(opPath,name);
toc
end
end